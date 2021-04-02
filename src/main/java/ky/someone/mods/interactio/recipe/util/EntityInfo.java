package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class EntityInfo extends CraftingInfo {
    private final LivingEntity entity;

    public EntityInfo(Level world, LivingEntity entity, JsonObject json) {
        super(world, json);
        this.entity = entity;
    }

    public LivingEntity getEntity() { return this.entity; }

    @Override public BlockPos getPos() { return getEntity().blockPosition(); }
}
