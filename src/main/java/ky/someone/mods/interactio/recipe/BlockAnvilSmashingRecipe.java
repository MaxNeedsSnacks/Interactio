package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import static ky.someone.mods.interactio.Utils.testAll;

public final class BlockAnvilSmashingRecipe extends InWorldRecipe<BlockPos, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();


    public BlockAnvilSmashingRecipe(ResourceLocation id, BlockIngredient blockInput, DynamicOutput output, JsonObject json) {
        super(id, null, blockInput, null, output, false, json);
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state, DefaultInfo info) {
        return this.blockInput.test(state.getBlock())
                && testAll(this.startCraftConditions, pos, state, info);
    }

    // anvilPos will be the position of the anvil
    // hitPos will be the position of the block hit
    @Override
    public void craft(BlockPos hitPos, DefaultInfo info) {
        craftBlock(this, hitPos, info);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_ANVIL;
    }

    @Override
    public boolean hasInvulnerableOutput() {
        return false;
    }

    private static class Serializer extends InWorldRecipeSerializer<BlockAnvilSmashingRecipe> {
        @Override
        public BlockAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"), "fluid");
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            return new BlockAnvilSmashingRecipe(id, input, output, json);
        }
    }
}
