package ky.someone.mods.interactio.recipe.base;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;

public abstract class StatelessItemRecipe<U extends CraftingInfo> extends StatelessRecipe<List<ItemEntity>, U> {

    public StatelessItemRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, JsonObject json)
    {
        super(id, itemInputs, blockInput, fluidInput, output, json);
    }
}