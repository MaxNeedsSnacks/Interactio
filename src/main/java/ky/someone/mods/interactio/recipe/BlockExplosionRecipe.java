package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.runAll;

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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockExplosionRecipe extends InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public BlockExplosionRecipe(ResourceLocation id, BlockIngredient blockInput, DynamicOutput output, JsonObject json) {
        super(id, null, blockInput, null, output, json);
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return this.blockInput.test(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        Level world = info.getWorld();

        // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
        world.destroyBlock(pos, false);

        runAll(this.preCraft, pos, info);
        // set it to the default state of our resulting block
        this.output.spawn(world, info.getPos());
        runAll(this.postCraft, pos, info);

        // don't let the explosion blow up the block we JUST placed
        explosion.getToBlow().remove(pos);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_EXPLODE;
    }

    private static class Serializer extends InWorldRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, input, output, json);
        }
    }
}
