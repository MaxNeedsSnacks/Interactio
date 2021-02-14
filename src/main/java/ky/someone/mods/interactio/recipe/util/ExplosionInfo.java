package ky.someone.mods.interactio.recipe.util;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public final class ExplosionInfo implements CraftingInfo {
    private final Level world;
    private final Explosion explosion;

    public ExplosionInfo(Level world, Explosion explosion) {
        this.world = world;
        this.explosion = explosion;
    }

    public Level getWorld() {
        return this.world;
    }

    public Explosion getExplosion() {
        return this.explosion;
    }
}
