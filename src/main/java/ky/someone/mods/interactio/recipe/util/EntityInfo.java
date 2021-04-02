package ky.someone.mods.interactio.recipe.util;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EntityInfo extends CraftingInfo {
    private final LivingEntity entity;

    public EntityInfo(InWorldRecipe<?,?,?> recipe, Level world, LivingEntity entity) {
        super(recipe, world);
        this.entity = entity;
    }

    public LivingEntity getEntity() { return this.entity; }

    @Override public Vec3 getPos() { return getEntity().position(); }
    @Override public BlockPos getBlockPos() { return getEntity().blockPosition(); }
}
