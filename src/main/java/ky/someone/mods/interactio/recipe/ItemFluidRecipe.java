package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.FluidState;

public class ItemFluidRecipe extends DurationRecipe<List<ItemEntity>, FluidState> {
    
    public static final Serializer SERIALIZER = new Serializer();
    
    protected final double consumeFluid;
    
    public ItemFluidRecipe(ResourceLocation id, List<ItemIngredient> inputs, FluidIngredient fluid, DynamicOutput output, boolean canRunParallel, double consumeFluid, int duration, JsonObject json)
    {
        super(id, inputs, null, fluid, output, canRunParallel, duration, json);
        this.consumeFluid = consumeFluid;
        
        this.postCraft.add(Events.events.get(new ResourceLocation("particle"))::accept);
    }
    
    @Override
    public boolean canCraft(List<ItemEntity> entities, FluidState state, DefaultInfo info)
    {
        return this.fluidInput.test(info.getWorld(), info.getPos())
                && compareStacks(entities, this.itemInputs)
                && testAll(this.startCraftConditions, entities, state, info);
    }
    
    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) { craftItemList(this, entities, info); }
    
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
        return InWorldRecipeType.ITEM_FLUID;
    }
    
    public double getConsumeFluid() { return this.consumeFluid; }
    
    @Override public boolean hasInvulnerableOutput() { return false; }

    public static class Serializer extends InWorldRecipeSerializer<ItemFluidRecipe>
    {
        @Override
        public ItemFluidRecipe fromJson(ResourceLocation id, JsonObject json)
        {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            FluidIngredient fluid = FluidIngredient.deserialize(json.get("fluid"));
            
            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");
            
            double consumeFluid = Utils.parseChance(json, "consumeFluid");
            
            boolean parallel = GsonHelper.getAsBoolean(json, "parallel", false);
            int duration = (int) Utils.getDouble(json, "duration", 0);
            
            return new ItemFluidRecipe(id, inputs, fluid, output, parallel, consumeFluid, duration, json);
        }
    }
}