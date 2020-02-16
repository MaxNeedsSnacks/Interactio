package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.event.ExplosionHandler.ExplosionInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import lombok.Value;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Objects;

@Value
public class BlockExplosionRecipe implements InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    Block result;
    Block input;
    double chance;
    boolean destroy;

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.equals(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();

        if (world.rand.nextDouble() < chance) {
            // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
            world.destroyBlock(pos, false);
            // set it to the default state of our resulting block
            world.setBlockState(pos, result.getDefaultState());
            // don't let the explosion blow up the block we JUST placed
            explosion.getAffectedBlockPositions().remove(pos);
        } else if (destroy) {
            // the craft has failed, but the recipe says we should destroy the block anyway
            world.destroyBlock(pos, false);
        }

    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(result.asItem());
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

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, JsonObject json) {

            Block result;
            if (Objects.requireNonNull(json.get("result"), "Result cannot be null!").isJsonObject()) {
                ItemStack stack = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));
                result = Block.getBlockFromItem(stack.getItem());
            } else {
                ResourceLocation loc = new ResourceLocation(JSONUtils.getString(json, "result"));
                result = ForgeRegistries.BLOCKS.getValue(loc);
            }

            if (result == null || result.equals(Blocks.AIR)) {
                throw new JsonParseException("Result is not a block!");
            }

            Block input;
            if (Objects.requireNonNull(json.get("input"), "Input cannot be null!").isJsonObject()) {
                ItemStack stack = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "input"));
                input = Block.getBlockFromItem(stack.getItem());
            } else {
                ResourceLocation loc = new ResourceLocation(JSONUtils.getString(json, "input"));
                input = ForgeRegistries.BLOCKS.getValue(loc);
            }

            if (input == null || input.equals(Blocks.AIR)) {
                throw new JsonParseException("Input is not a block!");
            }

            double chance = Utils.parseChance(json, "chance", 1);

            boolean destroy = JSONUtils.getBoolean(json, "destroy", false);

            return new BlockExplosionRecipe(id, result, input, chance, destroy);
        }

        @Nullable
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            Block result = buffer.readRegistryIdSafe(Block.class);
            Block input = buffer.readRegistryIdSafe(Block.class);

            double chance = buffer.readDouble();

            boolean destroy = buffer.readBoolean();

            return new BlockExplosionRecipe(id, result, input, chance, destroy);
        }

        @Override
        public void write(PacketBuffer buffer, BlockExplosionRecipe recipe) {
            buffer.writeRegistryId(recipe.result);
            buffer.writeRegistryId(recipe.input);

            buffer.writeDouble(recipe.chance);

            buffer.writeBoolean(recipe.destroy);
        }
    }
}
