package ky.someone.mods.interactio.recipe.base;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.StateHolder;

import javax.annotation.Nullable;
import java.util.List;

public abstract class StatelessRecipe<R, U extends CraftingInfo> extends InWorldRecipe<R, StateHolder<?, ?>, U> {

    public StatelessRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, JsonObject json) {
        super(id, itemInputs, blockInput, fluidInput, output, canRunParallel, json);
    }

    @Override
    @Deprecated // don't use this, obviously
    public boolean canCraft(R inputs, @Nullable StateHolder<?, ?> state, U info) {
        return canCraft(inputs, info);
    }

    public abstract boolean canCraft(R inputs, U info);
}