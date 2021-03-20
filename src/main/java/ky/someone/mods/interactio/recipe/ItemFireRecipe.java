package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;

public class ItemFireRecipe extends DurationRecipe<List<ItemEntity>, StateHolder<?, ?>> {
    
    public static final Serializer SERIALIZER = new Serializer();
    
    public ItemFireRecipe(ResourceLocation id, ItemIngredient input, DynamicOutput output, boolean canRunParallel, int duration, JsonObject json)
    {
        super(id, Arrays.asList(input), null, null, output, canRunParallel, duration, json);
        
        this.postCraft.add(Events.defaultItemEvents.get(new ResourceLocation("particle")));
    }
    
    @Override
    public boolean canCraft(Level world, List<ItemEntity> entities, StateHolder<?,?> state)
    {
        return testAll(this.startCraftConditions, entities, state)
                && compareStacks(entities, this.itemInputs);
    }
    
    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) { InWorldRecipe.craftItemList(this, entities, info); }
    
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }
    
    @Override
    public RecipeSerializer<?> getSerializer()
    {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType()
    {
        return InWorldRecipeType.FLUID_TRANSFORM;
    }
    
    @Override public boolean hasInvulnerableOutput() { return true; }
    
    public static class Serializer extends InWorldRecipeSerializer<ItemFireRecipe>
    {
        @Override
        public ItemFireRecipe fromJson(ResourceLocation id, JsonObject json)
        {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            
            ItemIngredient input = ItemIngredient.deserialize(json.get("input"));
            if (input.getIngredient().isEmpty())
                throw new JsonParseException(String.format("No valid input specified for recipe %s!", id));
            
            boolean parallel = GsonHelper.getAsBoolean(json, "parallel", true);
            int duration = (int) Utils.getDouble(json, "duration", 0);
            
            List<BiPredicate<List<ItemEntity>, FluidState>> startConditions = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, "startConditions", new JsonArray()).forEach(event -> {
                ResourceLocation loc = new ResourceLocation(event.getAsString());
                startConditions.add(Events.fluidItemPredicates.get(loc));
            });
            
            return new ItemFireRecipe(id, input, output, parallel, duration, json);
        }
    }
}