package ky.someone.mods.interactio.recipe.base;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.StateHolder;

public abstract class DurationRecipe<T, S extends StateHolder<?, ?>> extends InWorldRecipe<T, S, DefaultInfo> {

    protected final int duration;
    
    public DurationRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, int duration, JsonObject json)
    {
        super(id, itemInputs, blockInput, fluidInput, output, json);
        this.duration = duration;
    }
    
    public int getDuration() { return duration; }
    public boolean isFinished(int duration) { return duration > this.duration; }
}