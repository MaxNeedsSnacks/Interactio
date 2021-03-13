package ky.someone.mods.interactio.recipe.util;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

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
    public BlockPos getPos()
    {
        return new BlockPos(getExplosion().getPosition());
    }
}
