package dev.maxneedssnacks.interactio.core.mixin;

import dev.maxneedssnacks.interactio.core.InWorldCheckable;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity implements InWorldCheckable {

    @Shadow
    public abstract ItemStack getItem();

    private boolean interactio$isI2FInput;
    private boolean interactio$isF2FInput;

    private boolean interactio$inputsChecked;

    public ItemEntityMixin(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Inject(method = "setItem", at = @At(value = "RETURN"))
    public void interactio$invalidateInputs(CallbackInfo ci) {
        interactio$inputsChecked = false;
    }

    @Inject(method = "tick", at = @At(
            value = "FIELD",
            ordinal = 1,
            target = "Lnet/minecraft/entity/item/ItemEntity;ticksExisted:I",
            shift = At.Shift.BY,
            by = 4
    ))
    public void interactio$checkFluidRecipes(CallbackInfo ci) {
        if (!this.world.isRemote) {
            if (!interactio$inputsChecked) {
                interactio$updateValidInputs();
            } else {
                if (interactio$isI2FInput || interactio$isF2FInput) {
                    BlockPos pos = this.getPosition();
                    FluidState fluid = this.world.getFluidState(pos);
                    if (!fluid.isEmpty()) {
                        if (interactio$isI2FInput) {
                            List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class,
                                    this.getBoundingBox().grow(0.5D, 0.0D, 0.5D),
                                    e -> ((InWorldCheckable) e).isI2FInput());

                            InWorldRecipeType.ITEM_FLUID_TRANSFORM
                                    .apply(recipe -> recipe.canCraft(items, fluid),
                                            recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
                        }

                        if (interactio$isF2FInput) {
                            List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class,
                                    this.getBoundingBox().grow(0.5D, 0.0D, 0.5D),
                                    e -> ((InWorldCheckable) e).isF2FInput());

                            InWorldRecipeType.ITEM_FLUID_TRANSFORM
                                    .apply(recipe -> recipe.canCraft(items, fluid),
                                            recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
                        }
                    }
                }
            }
        }
    }

    public void interactio$updateValidInputs() {
        interactio$isI2FInput = InWorldRecipeType.ITEM_FLUID_TRANSFORM.isValidInput(this.getItem());
        interactio$isF2FInput = InWorldRecipeType.FLUID_FLUID_TRANSFORM.isValidInput(this.getItem());

        interactio$inputsChecked = true;
    }

    @Override
    public boolean isI2FInput() {
        return interactio$isI2FInput;
    }

    @Override
    public boolean isF2FInput() {
        return interactio$isF2FInput;
    }
}
