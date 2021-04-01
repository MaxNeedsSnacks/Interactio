package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockExplosionRecipe extends InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public BlockExplosionRecipe(ResourceLocation id, BlockIngredient blockInput, DynamicOutput output, JsonObject json) {
        super(id, null, blockInput, null, output, false, json);
        
        this.postCraft.add((pos, info) -> info.getExplosion().getToBlow().remove(pos));
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return this.blockInput.test(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) { craftBlock(this, pos, info); }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_EXPLODE;
    }
    
    @Override public boolean hasInvulnerableOutput() { return false; }

    private static class Serializer extends InWorldRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, input, output, json);
        }
    }
}
