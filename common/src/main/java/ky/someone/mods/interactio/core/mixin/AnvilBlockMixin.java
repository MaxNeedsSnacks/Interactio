package ky.someone.mods.interactio.core.mixin;

import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
            method = "onLand",
            at = @At(value = "RETURN")
    )
    public void handleAnvilRecipes(Level level, BlockPos pos, BlockState fallState, BlockState landState, FallingBlockEntity entity, CallbackInfo ci) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos.offset(1, 1, 1)));
        BlockPos hitPos = pos.below();
        BlockState hitState = level.getBlockState(hitPos);

        InWorldRecipeType.ITEM_ANVIL_SMASHING.applyAll(recipe -> recipe.canCraft(items, hitState),
                recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(level, pos)));

        InWorldRecipeType.BLOCK_ANVIL_SMASHING.apply(recipe -> recipe.canCraft(pos, hitState),
                recipe -> recipe.craft(pos, new InWorldRecipe.DefaultInfo(level, hitPos)));
    }

}
