package ky.someone.mods.interactio.recipe.util;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class DefaultInfo extends CraftingInfo {
    protected final BlockPos pos;

    public DefaultInfo(InWorldRecipe<?,?,?> recipe, Level world, BlockPos pos) {
        super(recipe, world);
        this.pos = pos;
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }
}