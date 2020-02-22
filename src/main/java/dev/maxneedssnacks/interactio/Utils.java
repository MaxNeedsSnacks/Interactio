package dev.maxneedssnacks.interactio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.maxneedssnacks.interactio.network.PacketCraftingParticle;
import dev.maxneedssnacks.interactio.recipe.util.IngredientStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.maxneedssnacks.interactio.Interactio.NETWORK;

public final class Utils {

    public static boolean isItem(Entity e) {
        return e instanceof ItemEntity;
    }

    @Nullable
    public static Fluid parseFluidNullable(String id) {
        return parseFluidNullable(new ResourceLocation(id));
    }

    @Nullable
    public static Fluid parseFluidNullable(ResourceLocation id) {
        return parseFluid(id).orElse(null);
    }

    public static Fluid parseFluidStrict(String id) {
        return parseFluidStrict(new ResourceLocation(id));
    }

    public static Fluid parseFluidStrict(ResourceLocation id) {
        return parseFluid(id).orElseThrow(() -> new RuntimeException("Unable to parse fluid with id " + id + "!"));
    }

    public static Optional<Fluid> parseFluid(String id) {
        return parseFluid(new ResourceLocation(id));
    }

    public static Optional<Fluid> parseFluid(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.FLUIDS.getValue(id));
    }

    // region recipe
    public static boolean compareStacks(List<ItemEntity> entities, List<IngredientStack> ingredients) {
        return compareStacks(entities, new Object2IntOpenHashMap<>(), ingredients);
    }

    public static boolean compareStacks(List<ItemEntity> entities, Object2IntMap<ItemEntity> used, List<IngredientStack> ingredients) {

        List<IngredientStack> required = ingredients.stream().map(IngredientStack::copy).collect(Collectors.toList());

        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();

            if (!entity.isAlive()) return false;

            for (IngredientStack req : required) {
                Ingredient ingredient = req.getIngredient();
                int needed = req.getCount();

                if (ingredient.test(item)) {
                    used.put(entity, Math.min(needed, item.getCount()));
                    req.shrink(item.getCount());
                    break;
                }
            }

            required.removeIf(IngredientStack::isEmpty);
        }

        return required.isEmpty();
    }
    //endregion recipe

    // region network
    // TODO: add custom particle support for datapacks
    public static void sendParticlePacket(World world, Entity entity) {
        sendParticlePacket(world, entity.getPositionVec());
    }

    public static void sendParticlePacket(World world, BlockPos pos) {
        sendParticlePacket(world, new Vec3d(pos));
    }

    public static void sendParticlePacket(World world, Vec3d pos) {
        PacketCraftingParticle packet = new PacketCraftingParticle(pos.x, pos.y, pos.z);
        sendPacketNear(packet, world, pos);
    }

    public static void sendPacketNear(Object packet, World world, Entity entity) {
        sendPacketNear(packet, world, entity.getPositionVec());
    }

    public static void sendPacketNear(Object packet, World world, BlockPos pos) {
        sendPacketNear(packet, world, new Vec3d(pos));
    }

    public static void sendPacketNear(Object packet, World world, Vec3d pos) {
        sendPacketInRadius(packet, world, pos, 64);
    }

    public static void sendPacketInRadius(Object packet, World world, Vec3d pos, int radius) {
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).getChunkProvider()
                    .chunkManager
                    .getTrackingPlayers(new ChunkPos(new BlockPos(pos)), false)
                    .filter(p -> p.getDistanceSq(pos.getX(), pos.getY(), pos.getZ()) < radius * radius)
                    .forEach(p -> NETWORK.send(PacketDistributor.PLAYER.with(() -> p), packet));
        }
    }

    public static double parseChance(JsonObject object, String key) {
        return parseChance(object, key, 0);
    }

    public static double parseChance(JsonObject object, String key, double dv) {
        if (!object.has(key)) return dv;

        JsonElement e = object.get(key);
        if (!e.isJsonPrimitive()) {
            Interactio.LOGGER.warn("Could not parse chance from " + key + " as it's not a primitive type!");
            return dv;
        }

        JsonPrimitive p = (JsonPrimitive) e;
        try {
            return MathHelper.clamp(p.getAsDouble(), 0, 1);
        } catch (Exception ex) {
            return p.getAsBoolean() ? 1 : 0;
        }
    }

    // shouldn't be needed, but who knows
    public static void ensureClientSide(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            throw new UnsupportedOperationException("Packet should only be handled on client!");
        }
    }
    // endregion network

    public static String translate(String langKey, @Nullable Style style, Object... replacements) {
        return new TranslationTextComponent(langKey, replacements).setStyle(style == null ? new Style() : style).getFormattedText();
    }

    public static ITextComponent formatChance(double chance, TextFormatting... styles) {
        return new StringTextComponent(String.format("%.2f%%", chance * 100.0)).applyTextStyles(styles);
    }

    /**
     * NOTE: This method originally stems from the Botania mod by Vazkii, which is Open Source
     * and distributed under the Botania License (see http://botaniamod.net/license.php)
     * <p>
     * Find the original Botania GitHub repository here: https://github.com/Vazkii/Botania
     * <p>
     * (Original class: vazkii.botania.client.integration.jei.petalapothecary.PetalApothecaryRecipeCategory, created by <williewillus>)
     */
    public static Point rotatePointAbout(Point in, Point about, double degrees) {
        double rad = degrees * Math.PI / 180.0;
        double newX = Math.cos(rad) * (in.x - about.x) - Math.sin(rad) * (in.y - about.y) + about.x;
        double newY = Math.sin(rad) * (in.x - about.x) + Math.cos(rad) * (in.y - about.y) + about.y;
        return new Point((int) Math.round(newX), (int) Math.round(newY));
    }
}
