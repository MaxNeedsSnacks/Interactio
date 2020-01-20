package dev.maxneedssnacks.interactio;

import dev.maxneedssnacks.interactio.network.PacketCraftingParticle;
import dev.maxneedssnacks.interactio.recipe.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.maxneedssnacks.interactio.Interactio.NETWORK;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

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

    // region entityconverters
    public static ObjectArrayList<ItemStack> entitiesToItemList(Collection<ItemEntity> entities) {
        return mapEntitiesToItems(entities)
                .collect(Collectors.toCollection(ObjectArrayList::new));
    }

    public static ObjectArrayList<ItemStack> copyEntitiesToItemList(Collection<ItemEntity> entities) {
        return mapEntitiesToItems(entities)
                .map(ItemStack::copy)
                .collect(Collectors.toCollection(ObjectArrayList::new));
    }

    public static Object2IntOpenHashMap<ItemStack> entitiesToItemMap(Collection<ItemEntity> entities) {
        return mapEntitiesToItems(entities)
                .collect(collectingAndThen(
                        toMap(Function.identity(), ItemStack::getCount, Integer::sum, Object2IntOpenHashMap::new),
                        (m -> {
                            m.keySet().forEach(s -> s.setCount(1));
                            return m;
                        })));
    }

    public static Object2IntOpenHashMap<ItemStack> copyEntitiesToItemMap(Collection<ItemEntity> entities) {
        return mapEntitiesToItems(entities)
                .collect(collectingAndThen(
                        toMap(ItemStack::copy, ItemStack::getCount, Integer::sum, Object2IntOpenHashMap::new),
                        (m -> {
                            m.keySet().forEach(s -> s.setCount(1));
                            return m;
                        })));
    }

    private static Stream<ItemStack> mapEntitiesToItems(Collection<ItemEntity> entities) {
        return entities.parallelStream()
                .filter(Objects::nonNull)
                .map(ItemEntity::getItem);
    }
    // endregion entityconverters

    // region recipe
    public static boolean compareStacks(List<ItemEntity> entities, Object2IntLinkedOpenHashMap<Ingredient> ingredients) {
        return compareStacks(entities, new Object2IntOpenHashMap<>(), ingredients);
    }

    public static boolean compareStacks(List<ItemEntity> entities, Object2IntMap<ItemEntity> used, Object2IntLinkedOpenHashMap<Ingredient> ingredients) {

        Object2IntLinkedOpenHashMap<Ingredient> required = ingredients.clone();

        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();

            if (!entity.isAlive()) return false;

            for (Entry<Ingredient> req : required.object2IntEntrySet()) {
                Ingredient ingredient = req.getKey();
                int needed = req.getIntValue();

                if (ingredient.test(item)) {
                    if (item.getCount() >= needed) {
                        used.put(entity, needed);
                        required.removeInt(ingredient);
                    } else {
                        used.put(entity, item.getCount());
                        required.put(ingredient, needed - item.getCount());
                    }
                    break;
                }
            }
        }

        return required.isEmpty();
    }

    public static <T extends InWorldRecipe<?, ?>> Optional<List<T>> getInWorldRecipeList(Class<T> clz) {
        return getInWorldRecipeStream(clz).map(s -> s.collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends InWorldRecipe<?, ?>> Optional<Stream<T>> getInWorldRecipeStream(Class<T> clz) {
        try {
            return Optional.of(ModRecipes.RECIPE_MAP
                    .get((IRecipeType<T>) clz.getField("RECIPE_TYPE").get(null))
                    .stream()
                    .filter(clz::isInstance)
                    .map(clz::cast));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    //endregion recipe

    public static double randomBetween(double origin, double bound, Random rand) {
        return origin + (bound - origin) * rand.nextDouble();
    }

    public static int randomBetween(int origin, int bound, Random rand) {
        return rand.nextInt(bound + origin) - origin;
    }

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

    // shouldn't be needed, but who knows
    public static void ensureClientSide(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            throw new UnsupportedOperationException("Packet should only be handled on client!");
        }
    }
    // endregion network

}
