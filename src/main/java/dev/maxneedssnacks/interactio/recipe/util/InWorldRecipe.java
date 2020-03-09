package dev.maxneedssnacks.interactio.recipe.util;

import dev.maxneedssnacks.interactio.recipe.ingredient.FluidIngredient;
import lombok.Value;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.state.IStateHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @param <T> A type of input or inputs. This will be what the recipe uses during crafts.
 * @param <S> Some kind of state that will be used to check whether a craft should be performed.
 * @param <U> A recipe info wrapper that will be used to provide information to {@link #craft(T, U)}
 */
public interface InWorldRecipe<T, S extends IStateHolder<?>, U extends CraftingInfo> extends IRecipe<IInventory> {

    /**
     * {@inheritDoc}
     * <p>
     * In-world recipes do not make use of any actual inventories.
     * This only exists is so we can have {@link net.minecraft.item.crafting.RecipeManager}
     * load all of our recipes correctly.
     *
     * @deprecated Use {@link #canCraft(T, S)} for validation instead.
     */
    @Override
    @Deprecated
    default boolean matches(IInventory inv, World worldIn) {
        return true;
    }

    /**
     * This is our analogue version to {@link IRecipe#matches(IInventory, World)}.
     * Use this to determine whether an in-world craft should be performed or not.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities)
     * @param state  State we want to check our inputs against.
     * @return Should this in-world craft be performed?
     */
    boolean canCraft(T inputs, S state);

    /**
     * Attempts to perform an in-world crafting recipe with the given parameters.
     * Beware: This does *not* necessarily check whether the craft can be performed first,
     * so make sure to run {@link #canCraft(T, S)} first if you want to ensure nothing goes wrong.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities).
     *               This object WILL be manipulated by this method,
     *               use {@link #canCraft(T, S)} if you don't want that to happen.
     * @param info   Additional information on the craft, like the world the craft is happening in or the affected Block's position
     */
    void craft(T inputs, U info);

    /**
     * {@inheritDoc}
     *
     * @deprecated see {@link #getRecipeOutput()}
     */
    @Override
    @Deprecated
    default ItemStack getCraftingResult(@Nullable IInventory inv) {
        return getRecipeOutput();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated In-world recipe outputs aren't necessarily single item stacks. Therefore, this method is unreliable and should be avoided.
     */
    @Override
    default ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As we don't use any inventory, no recipes would be able to "fit" anyways.
     */
    @Override
    @Deprecated
    default boolean canFit(int width, int height) {
        return false;
    }

    @Override
    default boolean isDynamic() {
        return true;
    }

    // ------------------------------------ //
    //  Implementation of common in-world   //
    //          recipe types below          //
    // ------------------------------------ //

    interface ItemsInFluid extends InWorldRecipe<List<ItemEntity>, IFluidState, DefaultInfo> {
        FluidIngredient getFluid();
    }

    interface Stateless<R, U extends CraftingInfo> extends InWorldRecipe<R, IStateHolder<?>, U> {
        @Override
        @Deprecated // don't use this, obviously
        default boolean canCraft(R inputs, @Nullable IStateHolder<?> state) {
            return canCraft(inputs);
        }

        boolean canCraft(R inputs);
    }

    interface ItemsStateless<U extends CraftingInfo> extends Stateless<List<ItemEntity>, U> {
    }

    // ------------------------------------ //
    //       "Default" crafting info        //
    //         (World and BlockPos)         //
    // ------------------------------------ //

    @Value
    class DefaultInfo implements CraftingInfo {
        World world;
        BlockPos pos;
    }

}
