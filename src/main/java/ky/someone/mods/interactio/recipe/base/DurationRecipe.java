package ky.someone.mods.interactio.recipe.base;

import static ky.someone.mods.interactio.Utils.runAll;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ky.someone.mods.interactio.Utils.RecipeTickEvent;
import ky.someone.mods.interactio.recipe.Events;
import ky.someone.mods.interactio.recipe.Events.EventType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.StateHolder;

public abstract class DurationRecipe<T, S extends StateHolder<?, ?>> extends InWorldRecipe<T, S, DefaultInfo> {

    protected Map<RecipeTickEvent<T, S>, JsonObject> tickConsumers;
    protected final int duration;
    
    public DurationRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, int duration, JsonObject json)
    {
        super(id, itemInputs, blockInput, fluidInput, output, canRunParallel, json);
        this.duration = duration;
        this.tickConsumers = new HashMap<>();
        
        this.parseTickEvents();
    }
    
    public void tick(T input, S state, DefaultInfo info)
    {
        runAll(this.tickConsumers, input, state, info);
    }
    
    public int getDuration() { return duration; }
    public boolean isFinished(int duration) { return duration > this.duration; }
    
    @SuppressWarnings("unchecked")
    private void parseTickEvents() {
        if (!json.has(EventType.TICK.jsonName)) return;
        JsonArray array = GsonHelper.getAsJsonArray(json, EventType.TICK.jsonName);
        for (Iterator<JsonElement> iter = array.iterator(); iter.hasNext();) {
            JsonElement element = iter.next();
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has("type")) continue;
            ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(object, "type"));
            
            RecipeTickEvent<T,S> event = (RecipeTickEvent<T,S>) Events.events.get(type);
            if (event != null) this.tickConsumers.put(event, object);
        }
    }
}