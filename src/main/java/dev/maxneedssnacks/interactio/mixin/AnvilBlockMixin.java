package dev.maxneedssnacks.interactio.mixin;

import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin extends FallingBlock {

    public AnvilBlockMixin(Properties p_i48401_1_) {
        super(p_i48401_1_);
        throw new IllegalStateException();
    }

    @Inject(
            method = "onEndFalling(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/item/FallingBlockEntity;)V",
            at = @At(value = "RETURN")
    )
    public void handleAnvilRecipes(World world, BlockPos pos, BlockState fallState, BlockState hitState, FallingBlockEntity entity, CallbackInfo ci) {
        List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(pos, pos.add(1, 1, 1)));

        InWorldRecipeType.ITEM_ANVIL_SMASHING.applyAll(recipe -> recipe.canCraft(items, hitState),
                recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
    }

}
