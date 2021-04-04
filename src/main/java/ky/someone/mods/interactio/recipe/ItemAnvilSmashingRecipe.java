package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
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
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

public final class ItemAnvilSmashingRecipe extends InWorldRecipe<List<ItemEntity>, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public ItemAnvilSmashingRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, JsonObject json) {
        super(id, inputs, null, null, output, true, json);
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, BlockState state, DefaultInfo info) {
        return compareStacks(entities, this.itemInputs)
                && testAll(this.startCraftConditions, entities, state, info);
    }

    @Override
    public void craft(List<ItemEntity> inputs, DefaultInfo info) {
        craftItemList(this, inputs, info);
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
        return InWorldRecipeType.ITEM_ANVIL;
    }

    @Override
    public boolean hasInvulnerableOutput() {
        return false;
    }

    public static class Serializer extends InWorldRecipeSerializer<ItemAnvilSmashingRecipe> {
        @Override
        public ItemAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"), "block", "fluid");

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            return new ItemAnvilSmashingRecipe(id, inputs, output, json);
        }
    }
}
