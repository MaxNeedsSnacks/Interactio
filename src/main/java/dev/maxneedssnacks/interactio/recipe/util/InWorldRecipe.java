package dev.maxneedssnacks.interactio.recipe.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.state.IStateHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @param <T> A type of input or inputs. This will be what the recipe uses during crafts.
 * @param <S> Some kind of state that will be used to check whether a craft should be performed.
 */
public abstract class InWorldRecipe<T, S extends IStateHolder<?>, U extends CraftingInfo> implements IRecipe<IInventory> {

    protected final ResourceLocation id;

    public InWorldRecipe(ResourceLocation id) {
        this.id = id;
    }

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
    public final boolean matches(IInventory inv, World worldIn) {
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
    public abstract boolean canCraft(T inputs, S state);

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
    public abstract void craft(T inputs, U info);

    /**
     * {@inheritDoc}
     *
     * @deprecated Use getRecipeOutput() instead as it doesn't require an inventory.
     */
    @Override
    @Deprecated
    public ItemStack getCraftingResult(@Nullable IInventory inv) {
        return getRecipeOutput().copy();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As we don't use any inventory, no recipes would be able to "fit" anyways.
     */
    @Override
    @Deprecated
    public final boolean canFit(int width, int height) {
        return false;
    }

    @Override
    public final ResourceLocation getId() {
        return id;
    }

    @Override
    public final boolean isDynamic() {
        return true;
    }

    // ------------------------------------ //
    //  Implementation of common in-world   //
    //          recipe types below          //
    // ------------------------------------ //

    public static abstract class ItemsInFluid extends InWorldRecipe<List<ItemEntity>, IFluidState, DefaultInfo> {
        public ItemsInFluid(ResourceLocation id) {
            super(id);
        }

        public abstract FluidIngredient getFluid();
    }

    public static abstract class Stateless<R, U extends CraftingInfo> extends InWorldRecipe<R, IStateHolder<?>, U> {

        public Stateless(ResourceLocation id) {
            super(id);
        }

        @Override
        @Deprecated // don't use this, obviously
        public final boolean canCraft(R inputs, IStateHolder<?> state) {
            return canCraft(inputs);
        }

        public abstract boolean canCraft(R inputs);
    }

    public static abstract class Items<U extends CraftingInfo> extends Stateless<List<ItemEntity>, U> {
        public Items(ResourceLocation id) {
            super(id);
        }
    }

    // ------------------------------------ //
    //       "Default" crafting info        //
    //         (World and BlockPos)         //
    // ------------------------------------ //

    @Getter
    @AllArgsConstructor
    public static class DefaultInfo implements CraftingInfo {
        final World world;
        final BlockPos pos;
    }

}
