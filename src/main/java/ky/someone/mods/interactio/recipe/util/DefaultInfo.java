package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class DefaultInfo extends CraftingInfo {
    protected final BlockPos pos;

    public DefaultInfo(Level world, BlockPos pos, JsonObject json) {
        super(world, json);
        this.pos = pos;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }
}