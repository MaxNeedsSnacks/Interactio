package ky.someone.mods.interactio.recipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public final class ExplosionInfo extends CraftingInfo {
    private final Explosion explosion;

    public ExplosionInfo(Level world, Explosion explosion) {
        super(world);
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
