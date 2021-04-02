package ky.someone.mods.interactio.recipe.util;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DefaultInfo extends CraftingInfo {
    protected final BlockPos pos;

    public DefaultInfo(InWorldRecipe<?,?,?> recipe, Level world, BlockPos pos) {
        super(recipe, world);
        this.pos = pos;
    }

    @Override
    public Vec3 getPos() {
        return Vec3.atCenterOf(this.pos);
    }
    
    @Override
    public BlockPos getBlockPos() {
        return this.pos;
    }
}