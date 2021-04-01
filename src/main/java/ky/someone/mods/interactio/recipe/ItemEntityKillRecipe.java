package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.base.StatelessItemRecipe;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.EntityIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.EntityInfo;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ItemEntityKillRecipe extends StatelessItemRecipe<EntityInfo> {

    public static final Serializer SERIALIZER = new Serializer();
    protected final EntityIngredient entityInput;

    public ItemEntityKillRecipe(ResourceLocation id, List<ItemIngredient> inputs, EntityIngredient entityInput, DynamicOutput output, JsonObject json) {
        super(id, inputs, null, null, output, true, json);
        this.entityInput = entityInput;
    }

    public boolean canCraft(LivingEntity entity, List<ItemEntity> entities) {
        return this.entityInput.test(entity) && canCraft(entities);
    }
    
    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return testAll(this.startCraftConditions, entities, null)
                && compareStacks(entities, this.itemInputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, EntityInfo info) {
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
        return InWorldRecipeType.ITEM_ENTITY_KILL;
    }
    
    @Override public boolean hasInvulnerableOutput() { return false; }

    public static class Serializer extends InWorldRecipeSerializer<ItemEntityKillRecipe> {
        @Override
        public ItemEntityKillRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");
            EntityIngredient entityInput = EntityIngredient.deserialize(json);

            return new ItemEntityKillRecipe(id, inputs, entityInput, output, json);
        }
    }
}
