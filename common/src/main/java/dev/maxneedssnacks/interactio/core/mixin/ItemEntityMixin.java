package dev.maxneedssnacks.interactio.core.mixin;

import dev.maxneedssnacks.interactio.core.IFluidRecipeInput;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
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
abstract class ItemEntityMixin extends Entity implements IFluidRecipeInput {

    @Shadow
    public abstract ItemStack getItem();

    private boolean interactio$isI2FInput;
    private boolean interactio$isF2FInput;

    private boolean interactio$inputsChecked;

    private boolean interactio$craftedLastTick = false;

    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "setItem", at = @At(value = "RETURN"))
    public void interactio$invalidateInputs(CallbackInfo ci) {
        interactio$inputsChecked = false;
    }

    @Inject(method = "tick", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;onGround:Z",
            ordinal = 0
    ))
    public void interactio$checkFluidRecipes(CallbackInfo ci) {
        if (!this.level.isClientSide && (interactio$craftedLastTick || tickCount % 5 == 0)) {
            if (isI2FInput() || isF2FInput()) {
                interactio$craftedLastTick = false;
                BlockPos pos = this.getOnPos();
                FluidState fluid = this.level.getFluidState(pos);
                if (!fluid.isEmpty()) {
                    if (isI2FInput()) {
                        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                                this.getBoundingBox().expandTowards(0.5D, 0.0D, 0.5D),
                                e -> ((IFluidRecipeInput) e).isI2FInput());

                        InWorldRecipeType.ITEM_FLUID_TRANSFORM
                                .apply(recipe -> recipe.canCraft(items, fluid),
                                        recipe -> {
                                            interactio$craftedLastTick = true;
                                            recipe.craft(items, new InWorldRecipe.DefaultInfo(level, pos));
                                        });
                    }

                    if (isF2FInput()) {
                        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                                this.getBoundingBox().expandTowards(0.5D, 0.0D, 0.5D),
                                e -> ((IFluidRecipeInput) e).isF2FInput());

                        InWorldRecipeType.FLUID_FLUID_TRANSFORM
                                .apply(recipe -> recipe.canCraft(items, fluid),
                                        recipe -> {
                                            interactio$craftedLastTick = true;
                                            recipe.craft(items, new InWorldRecipe.DefaultInfo(level, pos));
                                        });
                    }
                }
            }
        }
    }

    public void interactio$updateValidInputs() {
        if (!interactio$inputsChecked) {
            interactio$isI2FInput = InWorldRecipeType.ITEM_FLUID_TRANSFORM.isValidInput(this.getItem());
            interactio$isF2FInput = InWorldRecipeType.FLUID_FLUID_TRANSFORM.isValidInput(this.getItem());
            interactio$inputsChecked = true;
        }
    }

    @Override
    public boolean isI2FInput() {
        interactio$updateValidInputs();
        return interactio$isI2FInput;
    }

    @Override
    public boolean isF2FInput() {
        interactio$updateValidInputs();
        return interactio$isF2FInput;
    }
}
