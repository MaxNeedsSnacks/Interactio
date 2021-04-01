package ky.someone.mods.interactio.recipe.base;

import static ky.someone.mods.interactio.Utils.runAll;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

public abstract class DurationRecipe<T, S extends StateHolder<?, ?>> extends InWorldRecipe<T, S, DefaultInfo> {

    protected List<BiConsumer<T, S>> tickConsumers;
    protected final int duration;
    
    public DurationRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, int duration, JsonObject json)
    {
        super(id, itemInputs, blockInput, fluidInput, output, canRunParallel, json);
        this.duration = duration;
        this.tickConsumers = new LinkedList<>();
    }
    
    public abstract boolean canCraft(Level world, BlockPos pos, T inputs, S state);
    
    public void tick(T input, S state)
    {
        runAll(this.tickConsumers, input, state);
    }
    
    public int getDuration() { return duration; }
    public boolean isFinished(int duration) { return duration > this.duration; }
}