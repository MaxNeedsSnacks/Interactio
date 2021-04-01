package ky.someone.mods.interactio.core.mixin;

import java.util.LinkedList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ky.someone.mods.interactio.recipe.ItemFireRecipe;
import ky.someone.mods.interactio.recipe.ItemFluidRecipe;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.duration.DurationManager;
import ky.someone.mods.interactio.recipe.duration.RecipeDataTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity {

    @Shadow public abstract ItemStack getItem();
    
    private boolean checkedRecipeInput;
    private boolean isFireInput;
    
    public ItemEntityMixin(EntityType<?> type, Level level) { super(type, level); }
    
    @Inject(method = "tick", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;onGround:Z",
            ordinal = 0
    ))
    protected void checkRecipes(CallbackInfo ci)
    {
        if (this.level.isClientSide) return;
        
        ItemEntity entity = (ItemEntity) (Object) this;
        BlockPos pos = this.blockPosition();
        
        if (checkFluidRecipes(entity, pos)) return;
        if (checkFireRecipes(entity, pos)) return;
    }
    
    protected boolean checkFluidRecipes(ItemEntity entity, BlockPos pos)
    {
        FluidState fluid = this.level.getFluidState(pos);
        if (fluid.isEmpty())
            return false;
        if (!fluid.isSource())
            return false;
        
        addToTracker(InWorldRecipeType.ITEM_FLUID, ItemFluidRecipe.class, entity, pos, fluid);
        return true;
    }
    
    protected boolean checkFireRecipes(ItemEntity entity, BlockPos pos)
    {
        BlockState block = this.level.getBlockState(pos);
        
        if (!(block.getBlock() instanceof BaseFireBlock)) {
            return false;
        }
        
        if (!checkedRecipeInput) {
            isFireInput = InWorldRecipeType.ITEM_BURN.isValidInput(entity.getItem());
            checkedRecipeInput = true;
        }
        if (!isFireInput) return false;
        
        this.setInvulnerable(true);
        
        addToTracker(InWorldRecipeType.ITEM_BURN, ItemFireRecipe.class, entity, pos, block);
        return true;
    }
    
    private static <R extends DurationRecipe<List<ItemEntity>,S>, S extends StateHolder<?,?>> void addToTracker(InWorldRecipeType<R> storage, Class<R> recipe, ItemEntity entity, BlockPos pos, S state)
    {
        RecipeDataTracker<List<ItemEntity>,S,R> tracker = DurationManager.get(entity.level, storage, recipe).getTracker();
        List<ItemEntity> entityList = tracker.getInput(pos, LinkedList::new);
        if (!entityList.contains(entity)) entityList.add(entity);
        tracker.setState(pos, state);
    }
}
