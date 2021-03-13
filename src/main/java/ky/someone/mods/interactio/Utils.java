package ky.someone.mods.interactio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class Utils {

    public static boolean isItem(Entity e) {
        return e instanceof ItemEntity;
    }

    // region recipe
    public static boolean compareStacks(List<ItemEntity> entities, Collection<ItemIngredient> ingredients) {
        return compareStacks(entities, new Object2IntOpenHashMap<>(), ingredients);
    }

    public static boolean compareStacks(List<ItemEntity> entities, Object2IntMap<ItemEntity> used, Collection<ItemIngredient> ingredients) {

        Collection<ItemIngredient> required = new ObjectOpenHashSet<>();
        ingredients.forEach(i -> required.add(i.copy()));

        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();

            if (!entity.isAlive()) return false;

            for (ItemIngredient req : required) {
                Ingredient ingredient = req.getIngredient();
                int available = Math.min(req.getCount(), item.getCount());

                if (ingredient.test(item)) {
                    used.mergeInt(entity, available - req.roll(available), Integer::sum);
                    req.shrink(item.getCount());
                    break;
                }
            }

            required.removeIf(ItemIngredient::isEmpty);
        }

        return required.isEmpty();
    }

    public static void shrinkAndUpdate(Object2IntMap<ItemEntity> entities) {
        entities.forEach((entity, count) -> {
            entity.setNeverPickUp();

            ItemStack item = entity.getItem().copy();
            item.shrink(count);

            entity.setItem(item);

            entity.setDefaultPickUpDelay();
        });
    }

    public static <T> WeightedOutput<T> singleOrWeighted(JsonObject json, IEntrySerializer<T> serializer) {
        WeightedOutput<T> output = new WeightedOutput<>(0);
        try {
            output.add(serializer.read(json), 1);
        } catch (Exception e) {
            output = WeightedOutput.deserialize(json, serializer);
        }
        return output;
    }
    //endregion recipe

    // region network
    public static void sendParticle(ParticleOptions particle, Level world, Vec3 pos) {
        sendParticle(particle, world, pos, 5);
    }

    public static void sendParticle(ParticleOptions particle, Level world, Vec3 pos, int count) {
        if (world instanceof ServerLevel) {
            Random rand = world.random;

            double dx = rand.nextGaussian() / 50;
            double dy = rand.nextGaussian() / 50;
            double dz = rand.nextGaussian() / 50;

            ((ServerLevel) world).sendParticles(
                    particle,
                    pos.x - dx,
                    pos.y + Mth.nextDouble(rand, 0, 1 - dy),
                    pos.z - dz,
                    count,
                    dx,
                    dy,
                    dz,
                    rand.nextGaussian() / 50
            );
        }
    }

    public static double parseChance(JsonObject object, String key) {
        return parseChance(object, key, 0);
    }

    public static double parseChance(JsonObject object, String key, double dv) {
        try {
            return getDouble(object, key, dv);
        } catch (Exception ex) {
            return GsonHelper.getAsBoolean(object, key, dv == 1) ? 1 : 0;
        }
    }

    public static double getDouble(JsonObject object, String key) {
        if (object.has(key)) {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                return e.getAsDouble();
            } else {
                throw new JsonSyntaxException("Could not parse double from " + key + " as it's not a number!");
            }
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Double");
        }
    }

    public static double getDouble(JsonObject object, String key, double dv) {
        return object.has(key) ? getDouble(object, key) : dv;
    }

    // shouldn't be needed, but who knows
    public static void ensureClientSide(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            throw new UnsupportedOperationException("Packet should only be handled on client!");
        }
    }
    // endregion network

    public static Component translate(String langKey, @Nullable Style style, Object... replacements) {
        return new TranslatableComponent(langKey, replacements).setStyle(style == null ? Style.EMPTY : style);
    }

    public static Component formatChance(double chance, ChatFormatting... styles) {
        return new TextComponent(String.format("%.2f%%", chance * 100.0)).withStyle(styles);
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
    
    public static <T, U> void runAll(List<BiConsumer<T, U>> events, T t, U u)
    {
        events.forEach(consumer -> consumer.accept(t, u));
    }
    
    public static <T, U> boolean testAll(List<BiPredicate<T, U>> events, T t, U u)
    {
        return events.stream().allMatch(predicate -> predicate.test(t, u));
    }
}
