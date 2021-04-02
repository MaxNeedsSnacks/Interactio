package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.testAll;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.EntityIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.EntityInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockEntityKillRecipe extends InWorldRecipe<BlockPos, BlockState, EntityInfo> {

    public static final Serializer SERIALIZER = new Serializer();
    protected final EntityIngredient entityInput;

    public BlockEntityKillRecipe(ResourceLocation id, BlockIngredient blockInput, EntityIngredient entityInput, DynamicOutput output, JsonObject json) {
        super(id, null, blockInput, null, output, true, json);
        this.entityInput = entityInput;
    }

    public boolean canCraft(LivingEntity entity, BlockPos pos, BlockState state, EntityInfo info) {
        return this.entityInput.test(entity) && canCraft(pos, state, info);
    }
    
    @Override
    public boolean canCraft(BlockPos pos, BlockState state, EntityInfo info) {
        return this.blockInput.test(state.getBlock())
                && testAll(this.startCraftConditions, pos, state, info);
    }

    @Override public void craft(BlockPos pos, EntityInfo info) { craftBlock(this, pos, info); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return InWorldRecipeType.BLOCK_ENTITY_KILL; }
    @Override public boolean hasInvulnerableOutput() { return false; }
    public EntityIngredient getEntityInput() { return this.entityInput; }

    public static class Serializer extends InWorldRecipeSerializer<BlockEntityKillRecipe> {
        @Override
        public BlockEntityKillRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            BlockIngredient blockInput = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));
            EntityIngredient entityInput = EntityIngredient.deserialize(GsonHelper.getAsJsonObject(json, "entity"));

            return new BlockEntityKillRecipe(id, blockInput, entityInput, output, json);
        }
    }
}
