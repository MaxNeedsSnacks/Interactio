package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.sendParticle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class Events
{
    public static Map<ResourceLocation, BiPredicate<List<ItemEntity>, FluidState>> fluidItemPredicates = new HashMap<>();
    public static Map<ResourceLocation, BiPredicate<BlockPos, BlockState>> fluidBlockPredicates = new HashMap<>();
    public static Map<ResourceLocation, BiPredicate<List<ItemEntity>, FluidState>> blockItemPredicates = new HashMap<>();
    public static Map<ResourceLocation, BiPredicate<BlockPos, BlockState>> blockBlockPredicates = new HashMap<>();
    
    public static Map<ResourceLocation, BiConsumer<List<ItemEntity>, DefaultInfo>> defaultItemEvents = new HashMap<>();
    public static Map<ResourceLocation, BiConsumer<BlockPos, DefaultInfo>> defaultBlockEvents = new HashMap<>();
    public static Map<ResourceLocation, BiConsumer<List<ItemEntity>, ExplosionInfo>> explosionItemEvents = new HashMap<>();
    public static Map<ResourceLocation, BiConsumer<BlockPos, ExplosionInfo>> explosionBlockEvents = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public static void init()
    {
        BiConsumer<?, ? extends CraftingInfo> spawnParticles = (arg, info) -> {
            Level world = info.getWorld();
            BlockPos pos;
            if (info instanceof DefaultInfo) pos = ((DefaultInfo) info).getPos();
            else if (info instanceof ExplosionInfo) pos = new BlockPos(((ExplosionInfo) info).getExplosion().getPosition());
            else throw new IllegalArgumentException("Info must be of type DefaultInfo or ExplosionInfo");
            Random rand = world.random;
            double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

            sendParticle(ParticleTypes.END_ROD, world, new Vec3(x, y, z));
        };
        ResourceLocation spawnParticlesLoc = new ResourceLocation("particle");
        
        defaultItemEvents.put(spawnParticlesLoc, (BiConsumer<List<ItemEntity>, DefaultInfo>) spawnParticles);
        defaultBlockEvents.put(spawnParticlesLoc, (BiConsumer<BlockPos, DefaultInfo>) spawnParticles);
        explosionItemEvents.put(spawnParticlesLoc, (BiConsumer<List<ItemEntity>, ExplosionInfo>) spawnParticles);
        explosionBlockEvents.put(spawnParticlesLoc, (BiConsumer<BlockPos, ExplosionInfo>) spawnParticles);
    }
}
