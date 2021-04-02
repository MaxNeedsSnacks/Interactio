package ky.someone.mods.interactio.recipe.util;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ExplosionInfo extends CraftingInfo {
    private final Explosion explosion;

    public ExplosionInfo(InWorldRecipe<?,?,?> recipe, Level world, Explosion explosion) {
        super(recipe, world);
        this.explosion = explosion;
    }

    public Explosion getExplosion() {
        return this.explosion;
    }

    @Override
    public Vec3 getPos()
    {
        return getExplosion().getPosition();
    }
    
    @Override
    public BlockPos getBlockPos()
    {
        return new BlockPos(getExplosion().getPosition());
    }
}
