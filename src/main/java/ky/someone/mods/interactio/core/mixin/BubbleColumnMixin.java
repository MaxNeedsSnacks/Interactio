package ky.someone.mods.interactio.core.mixin;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ky.someone.mods.interactio.recipe.ItemFluidRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.duration.RecipeManager;
import ky.someone.mods.interactio.recipe.duration.RecipeTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

@Mixin(BubbleColumnBlock.class)
abstract class BubbleColumnMixin {

    @Inject(method = "entityInside", at = @At("HEAD"))
    protected void registerItemEntities(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci)
    {
        if (!(entity instanceof ItemEntity))
            return;
        FluidState fluid = world.getFluidState(pos);
        if (!fluid.isSource())
            return;
        
        RecipeTracker<List<ItemEntity>, FluidState, ItemFluidRecipe> tracker = RecipeManager.get(world, InWorldRecipeType.FLUID_TRANSFORM, ItemFluidRecipe.class).getTracker();
        List<ItemEntity> entityList = tracker.getInput(pos, LinkedList::new);
        if (!entityList.contains(entity)) entityList.add((ItemEntity) entity);
        
        tracker.setState(pos, fluid);
    }
}
