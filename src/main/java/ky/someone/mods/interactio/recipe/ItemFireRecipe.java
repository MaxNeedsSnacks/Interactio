package ky.someone.mods.interactio.recipe;

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
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

public class ItemFireRecipe extends DurationRecipe<List<ItemEntity>, BlockState> {

    public static final Serializer SERIALIZER = new Serializer();

    public ItemFireRecipe(ResourceLocation id, ItemIngredient input, DynamicOutput output, boolean canRunParallel, int duration, JsonObject json) {
        super(id, Collections.singletonList(input), null, null, output, canRunParallel, duration, json);
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, BlockState state, DefaultInfo info) {
        return state.getBlock() instanceof BaseFireBlock
                && compareStacks(entities, this.itemInputs)
                && testAll(this.startCraftConditions, entities, state, info);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        InWorldRecipe.craftItemList(this, entities, info);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.ITEM_BURN;
    }

    @Override
    public boolean hasInvulnerableOutput() {
        return true;
    }

    public static class Serializer extends InWorldRecipeSerializer<ItemFireRecipe> {
        @Override
        public ItemFireRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"), "block", "fluid");

            ItemIngredient input = ItemIngredient.deserialize(json.get("input"));
            if (input.getIngredient().isEmpty())
                throw new JsonParseException(String.format("No valid input specified for recipe %s!", id));

            boolean parallel = GsonHelper.getAsBoolean(json, "parallel", true);
            int duration = (int) Utils.getDouble(json, "duration", 0);

            return new ItemFireRecipe(id, input, output, parallel, duration, json);
        }
    }
}