package ky.someone.interactio.recipe.util;

import ky.someone.interactio.recipe.ingredient.FluidIngredient;
import me.shedaniel.architectury.core.AbstractRecipeSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @param <T> A type of input or inputs. This will be what the recipe uses during crafts.
 * @param <S> Some kind of state that will be used to check whether a craft should be performed.
 * @param <U> A recipe info wrapper that will be used to provide information to {@link #craft(T, U)}
 */
public interface InWorldRecipe<T, S extends StateHolder<?, ?>, U extends CraftingInfo> extends Recipe<Container> {

    /**
     * {@inheritDoc}
     * <p>
     * In-world recipes do not make use of any actual inventories.
     * This only exists is so we can have {@link net.minecraft.world.item.crafting.RecipeManager}
     * load all of our recipes correctly.
     *
     * @deprecated Use {@link #canCraft(T, S)} for validation instead.
     */
    @Override
    @Deprecated
    default boolean matches(Container container, Level level) {
        return true;
    }

    /**
     * This is our analogue version to {@link Recipe#matches(Container, Level)}.
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

    @Override
    AbstractRecipeSerializer<?> getSerializer();

    @Override
    InWorldRecipeType<?> getType();

    /**
     * {@inheritDoc}
     *
     * @deprecated see {@link #getResultItem()} ()}
     */
    @Override
    @Deprecated
    default ItemStack assemble(@Nullable Container container) {
        return getResultItem();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated In-world recipe outputs aren't necessarily single item stacks. Therefore, this method is unreliable and should be avoided.
     */
    @Override
    @Deprecated
    default ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As we don't use any inventory, no recipes would be able to "fit" anyways.
     */
    @Override
    @Deprecated
    default boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    // ------------------------------------ //
    //  Implementation of common in-world   //
    //          recipe types below          //
    // ------------------------------------ //

    interface ItemsInFluid extends InWorldRecipe<List<ItemEntity>, FluidState, DefaultInfo> {
        FluidIngredient getFluid();
    }

    interface Stateless<R, U extends CraftingInfo> extends InWorldRecipe<R, StateHolder<?, ?>, U> {
        @Override
        @Deprecated // don't use this, obviously
        default boolean canCraft(R inputs, @Nullable StateHolder<?, ?> state) {
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

    final class DefaultInfo implements CraftingInfo {
        private final Level level;
        private final BlockPos pos;

        public DefaultInfo(Level level, BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        public Level getWorld() {
            return this.level;
        }

        public BlockPos getPos() {
            return this.pos;
        }
    }

}
