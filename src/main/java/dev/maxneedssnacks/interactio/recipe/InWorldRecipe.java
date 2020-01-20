package dev.maxneedssnacks.interactio.recipe;

import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
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
 * @param <S> Some kind of object that will be used to check whether a craft should be performed.
 */
public abstract class InWorldRecipe<T, S> implements IRecipe<IInventory> {

    protected final ResourceLocation id;

    public InWorldRecipe(ResourceLocation id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     *
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
     *
     * This is our analogue version to {@link IRecipe#matches(IInventory, World)}.
     * Use this to determine whether an in-world craft should be performed or not.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities)
     * @param state Object we want to check our inputs against.
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
     * @param world  World which the craft is happening in, we use this to manipulate the world (for example setting blocks to air).
     * @param pos    Block position where the craft is happening. This is the block we are going to manipulate.
     */
    public abstract void craft(T inputs, World world, BlockPos pos);

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

    public static abstract class ItemsInFluid extends InWorldRecipe<List<ItemEntity>, IFluidState> {
        public ItemsInFluid(ResourceLocation id) {
            super(id);
        }

        public abstract FluidIngredient getFluid();
    }


}
