package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.Utils;
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

public final class BlockLightningRecipe extends InWorldRecipe<BlockPos, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final double chance;

    public BlockLightningRecipe(ResourceLocation id, BlockIngredient blockInput, DynamicOutput output, double chance, JsonObject json) {
        super(id, null, blockInput, null, output, false, json);
        this.chance = chance;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state, DefaultInfo info) {
        return this.blockInput.test(state.getBlock())
                && testAll(this.startCraftConditions, pos, state, info);
    }

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
        return InWorldRecipeType.BLOCK_LIGHTNING;
    }

    public double getChance() {
        return this.chance;
    }

    @Override
    public boolean hasInvulnerableOutput() {
        return true;
    }

    private static class Serializer extends InWorldRecipeSerializer<BlockLightningRecipe> {
        @Override
        public BlockLightningRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            double chance = Utils.parseChance(json, "chance");

            return new BlockLightningRecipe(id, input, output, chance, json);
        }
    }
}
