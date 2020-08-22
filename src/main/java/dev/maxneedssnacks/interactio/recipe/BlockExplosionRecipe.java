package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.event.ExplosionHandler.ExplosionInfo;
import dev.maxneedssnacks.interactio.recipe.ingredient.BlockIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

public final class BlockExplosionRecipe implements InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final Output output;
    private final BlockIngredient input;

    public BlockExplosionRecipe(ResourceLocation id, Output output, BlockIngredient input) {
        this.id = id;
        this.output = output;
        this.input = input;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.test(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();
        Random rand = world.rand;

        // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
        world.destroyBlock(pos, false);

        // set it to the default state of our resulting block
        if (output.isBlock()) {
            Block block = output.getBlock();
            if (block != null) world.setBlockState(pos, block.getDefaultState());
        } else if (output.isItem()) {
            Collection<ItemStack> stacks = output.getItems();
            if (stacks != null) {
                double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
                double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
                double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

                double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

                stacks.forEach(stack -> {
                    ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                    newItem.setMotion(0, vel, 0);
                    newItem.setPickupDelay(20);
                    world.addEntity(newItem);
                });
            }
        }

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

    public Output getOutput() {
        return this.output;
    }

    public BlockIngredient getInput() {
        return this.input;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, JsonObject json) {
            Output output = Output.create(JSONUtils.getJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(JSONUtils.getJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, output, input);
        }

        @Nullable
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            Output output = Output.read(buffer);
            BlockIngredient input = BlockIngredient.read(buffer);

            return new BlockExplosionRecipe(id, output, input);
        }

        @Override
        public void write(PacketBuffer buffer, BlockExplosionRecipe recipe) {
            recipe.output.write(buffer);
            recipe.input.write(buffer);
        }
    }

    public static final class Output {
        public final WeightedOutput<Block> blockOutput;
        public final WeightedOutput<ItemStack> itemOutput;

        @Nullable
        public Block getBlock() {
            return isBlock() ? blockOutput.rollOnce() : null;
        }

        @Nullable
        public Collection<ItemStack> getItems() {
            return isItem() ? itemOutput.roll() : null;
        }

        public boolean isBlock() {
            return blockOutput != null;
        }

        public boolean isItem() {
            return itemOutput != null;
        }

        private Output(@Nullable WeightedOutput<Block> blockOutput, @Nullable WeightedOutput<ItemStack> itemOutput) {
            if (blockOutput != null && itemOutput != null)
                throw new IllegalArgumentException("Either block OR item should be provided, not both!");
            if (blockOutput == null && itemOutput == null)
                throw new IllegalArgumentException("Either block OR item should be provided!");
            this.blockOutput = blockOutput;
            this.itemOutput = itemOutput;
        }

        private static Output create(JsonObject json) {
            // 4 cases to check
            if (json.has("block")) {
                // single block
                return new Output(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), null);
            } else if (json.has("item")) {
                // single item
                return new Output(null, Utils.singleOrWeighted(json, IEntrySerializer.ITEM));
            } else {
                // assume it's a weighted output
                // try to get a type variable, or error otherwise
                if (json.has("type")) {
                    switch (JSONUtils.getString(json, "type")) {
                        case "item":
                            return new Output(null, Utils.singleOrWeighted(json, IEntrySerializer.ITEM));
                        case "block":
                            return new Output(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), null);
                        default:
                            throw new JsonSyntaxException("Unsupported type for output on block explosion recipe!");
                    }
                } else {
                    throw new JsonSyntaxException("Weighted output types are ambiguous -- please add a 'type' attribute to clarify which type of output you want!");
                }
            }
        }

        void write(PacketBuffer buf) {
            if (isItem()) {
                buf.writeByte(0);
                itemOutput.write(buf, IEntrySerializer.ITEM);
            } else if (isBlock()) {
                buf.writeByte(1);
                blockOutput.write(buf, IEntrySerializer.BLOCK);
            } else throw new IllegalStateException("Wrong output type!");
        }

        static Output read(PacketBuffer buf) {
            switch (buf.readByte()) {
                // item
                case 0:
                    return new Output(null, WeightedOutput.read(buf, IEntrySerializer.ITEM));
                // block
                case 1:
                    return new Output(WeightedOutput.read(buf, IEntrySerializer.BLOCK), null);
                // error
                default:
                    throw new IllegalStateException("Wrong output type id!");
            }
        }
    }
}
