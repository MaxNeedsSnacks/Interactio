package ky.someone.mods.interactio.recipe;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.base.StatelessItemRecipe;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ItemLightningRecipe extends StatelessItemRecipe<DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public ItemLightningRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, JsonObject json) {
        super(id, inputs, null, null, output, true, json);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.ITEM_LIGHTNING;
    }

    @Override public boolean hasInvulnerableOutput() { return true; }

    public static class Serializer extends InWorldRecipeSerializer<ItemLightningRecipe> {
        @Override
        public ItemLightningRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            return new ItemLightningRecipe(id, inputs, output, json);
        }
    }
}
