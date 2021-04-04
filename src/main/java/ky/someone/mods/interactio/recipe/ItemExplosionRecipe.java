package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.base.StatelessItemRecipe;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

public final class ItemExplosionRecipe extends StatelessItemRecipe<ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public ItemExplosionRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, JsonObject json) {
        super(id, inputs, null, null, output, true, json);
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, ExplosionInfo info) {
        return compareStacks(entities, this.itemInputs)
                && testAll(this.startCraftConditions, entities, null, info);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionInfo info) {
        craftItemList(this, entities, info);
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
        return InWorldRecipeType.ITEM_EXPLODE;
    }

    @Override
    public boolean hasInvulnerableOutput() {
        return false;
    }

    public static class Serializer extends InWorldRecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            return new ItemExplosionRecipe(id, inputs, output, json);
        }
    }
}
