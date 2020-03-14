package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.event.ExplosionHandler.ExplosionInfo;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public final class BlockExplosionRecipe implements InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<Block> output;
    private final Block input;

    public BlockExplosionRecipe(ResourceLocation id, WeightedOutput<Block> output, Block input) {
        this.id = id;
        this.output = output;
        this.input = input;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.equals(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();

        // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
        world.destroyBlock(pos, false);

        // set it to the default state of our resulting block
        Block block = output.rollOnce();
        if (block != null) world.setBlockState(pos, block.getDefaultState());

        // don't let the explosion blow up the block we JUST placed
        explosion.getAffectedBlockPositions().remove(pos);

    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_EXPLODE;
    }

    public WeightedOutput<Block> getOutput() {
        return this.output;
    }

    public Block getInput() {
        return this.input;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, JsonObject json) {
            WeightedOutput<Block> output = Utils.singleOrWeighted(JSONUtils.getJsonObject(json, "output"), IEntrySerializer.BLOCK);
            Block input = IEntrySerializer.BLOCK.read(JSONUtils.getJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, output, input);
        }

        @Nullable
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            WeightedOutput<Block> output = WeightedOutput.read(buffer, IEntrySerializer.BLOCK);
            Block input = IEntrySerializer.BLOCK.read(buffer);

            return new BlockExplosionRecipe(id, output, input);
        }

        @Override
        public void write(PacketBuffer buffer, BlockExplosionRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.BLOCK);
            IEntrySerializer.BLOCK.write(buffer, recipe.input);
        }
    }
}
