package ky.someone.mods.interactio.recipe.base;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

public abstract class StatelessItemRecipe<U extends CraftingInfo> extends StatelessRecipe<List<ItemEntity>, U> {

    public StatelessItemRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, JsonObject json) {
        super(id, itemInputs, blockInput, fluidInput, output, canRunParallel, json);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, U info) {
        return testAll(this.startCraftConditions, entities, null, info)
                && compareStacks(entities, this.itemInputs);
    }

    @Override
    public void craft(List<ItemEntity> inputs, U info) {
        craftItemList(this, inputs, info);
    }
}