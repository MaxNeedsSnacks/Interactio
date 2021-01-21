package ky.someone.interactio.core.mixin;

import ky.someone.interactio.core.IWCFluidRecipeInput;
import ky.someone.interactio.recipe.util.InWorldRecipe;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity implements IWCFluidRecipeInput {

    @Shadow
    public abstract ItemStack getItem();

    private boolean iwc$isI2FInput;
    private boolean iwc$isF2FInput;

    private boolean iwc$inputsChecked;

    private boolean iwc$craftedLastTick = false;

    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "setItem", at = @At(value = "RETURN"))
    public void iwc$invalidateInputs(CallbackInfo ci) {
        iwc$inputsChecked = false;
    }

    @Inject(method = "tick", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;onGround:Z",
            ordinal = 0
    ))
    public void iwc$checkFluidRecipes(CallbackInfo ci) {
        if (!this.level.isClientSide && (iwc$craftedLastTick || tickCount % 5 == 0)) {
            if (iwc$isI2FInput() || iwc$isF2FInput()) {
                iwc$craftedLastTick = false;
                BlockPos pos = this.getOnPos();
                FluidState fluid = this.level.getFluidState(pos);
                if (!fluid.isEmpty()) {
                    if (iwc$isI2FInput()) {
                        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                                this.getBoundingBox().inflate(1),
                                e -> ((IWCFluidRecipeInput) e).iwc$isI2FInput() && e.blockPosition().equals(blockPosition()));

                        InWorldRecipeType.ITEM_FLUID_TRANSFORM
                                .apply(recipe -> recipe.canCraft(items, fluid),
                                        recipe -> {
                                            iwc$craftedLastTick = true;
                                            recipe.craft(items, new InWorldRecipe.DefaultInfo(level, pos));
                                        });
                    }

                    if (iwc$isF2FInput()) {
                        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                                this.getBoundingBox().inflate(1),
                                e -> ((IWCFluidRecipeInput) e).iwc$isF2FInput() && e.blockPosition().equals(blockPosition()));

                        InWorldRecipeType.FLUID_FLUID_TRANSFORM
                                .apply(recipe -> recipe.canCraft(items, fluid),
                                        recipe -> {
                                            iwc$craftedLastTick = true;
                                            recipe.craft(items, new InWorldRecipe.DefaultInfo(level, pos));
                                        });
                    }
                }
            }
        }
    }

    public void iwc$updateValidInputs() {
        if (!iwc$inputsChecked) {
            iwc$isI2FInput = InWorldRecipeType.ITEM_FLUID_TRANSFORM.isValidInput(this.getItem());
            iwc$isF2FInput = InWorldRecipeType.FLUID_FLUID_TRANSFORM.isValidInput(this.getItem());
            iwc$inputsChecked = true;
        }
    }

    @Override
    public boolean iwc$isI2FInput() {
        iwc$updateValidInputs();
        return iwc$isI2FInput;
    }

    @Override
    public boolean iwc$isF2FInput() {
        iwc$updateValidInputs();
        return iwc$isF2FInput;
    }
}
