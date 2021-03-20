package ky.someone.mods.interactio.core.mixin;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import ky.someone.mods.interactio.recipe.ItemFluidRecipe;
import ky.someone.mods.interactio.recipe.duration.RecipeTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

@Mixin(LiquidBlock.class)
abstract class LiquidBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"))
    protected void registerItemEntities(BlockState state, Level world, BlockPos pos, Entity entity)
    {
        if (!(entity instanceof ItemEntity))
            return;
        if (!world.getFluidState(pos).isSource())
            return;
        RecipeTracker<List<ItemEntity>, FluidState, ItemFluidRecipe> tracker = RecipeTracker.get(world, ItemFluidRecipe.class);
        List<ItemEntity> entityList = tracker.getInput(pos, () -> new LinkedList<>());
        if (!entityList.contains(entity)) entityList.add((ItemEntity) entity);
    }
}
