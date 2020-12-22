package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ingredient.BlockIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.BlockOrItemOutput;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticle;

public final class BlockAnvilSmashingRecipe implements InWorldRecipe<BlockPos, BlockState, InWorldRecipe.DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final BlockOrItemOutput output;
    private final BlockIngredient input;
    private final double damage;

    public BlockAnvilSmashingRecipe(ResourceLocation id, BlockOrItemOutput output, BlockIngredient input, double damage) {
        this.id = id;
        this.output = output;
        this.input = input;
        this.damage = damage;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.test(state.getBlock());
    }

    // anvilPos will be the position of the anvil
    // hitPos will be the position of the block hit
    @Override
    public void craft(BlockPos anvilPos, DefaultInfo info) {
        World world = info.getWorld();
        Random rand = world.getRandom();
        BlockPos hitPos = info.getPos();

        // set it to the default state of our resulting block
        if (output.isBlock()) {
            Block block = output.getBlock();
            if (block != null) world.setBlockState(hitPos, block.getDefaultState());
        } else if (output.isItem()) {
            world.destroyBlock(hitPos, false);
            Collection<ItemStack> stacks = output.getItems();
            if (stacks != null) {
                double x = hitPos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
                double y = hitPos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
                double z = hitPos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

                double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

                stacks.forEach(stack -> {
                    ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                    newItem.setMotion(0, vel, 0);
                    newItem.setPickupDelay(20);
                    world.addEntity(newItem);
                });
            }
        }

        // damage anvil
        if (rand.nextDouble() < damage) {
            sendParticle(new BlockParticleData(ParticleTypes.BLOCK, world.getBlockState(anvilPos)), world, Vector3d.copyCenteredHorizontally(anvilPos), 25);
            BlockState dmg = AnvilBlock.damage(world.getBlockState(anvilPos));
            if (dmg == null) {
                world.setBlockState(anvilPos, Blocks.AIR.getDefaultState());
                world.playEvent(1029, anvilPos, 0);
            } else {
                world.setBlockState(anvilPos, dmg);
            }
        }
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
        return InWorldRecipeType.BLOCK_ANVIL_SMASHING;
    }

    public BlockOrItemOutput getOutput() {
        return this.output;
    }

    public BlockIngredient getInput() {
        return this.input;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlockAnvilSmashingRecipe> {
        @Override
        public BlockAnvilSmashingRecipe read(ResourceLocation id, JsonObject json) {
            BlockOrItemOutput output = BlockOrItemOutput.create(JSONUtils.getJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(JSONUtils.getJsonObject(json, "input"));

            double damage = Utils.parseChance(json, "damage");

            return new BlockAnvilSmashingRecipe(id, output, input, damage);
        }

        @Nullable
        @Override
        public BlockAnvilSmashingRecipe read(ResourceLocation id, PacketBuffer buffer) {
            BlockOrItemOutput output = BlockOrItemOutput.read(buffer);
            BlockIngredient input = BlockIngredient.read(buffer);

            double damage = buffer.readDouble();

            return new BlockAnvilSmashingRecipe(id, output, input, damage);
        }

        @Override
        public void write(PacketBuffer buffer, BlockAnvilSmashingRecipe recipe) {
            recipe.output.write(buffer);
            recipe.input.write(buffer);

            buffer.writeDouble(recipe.damage);
        }
    }

}
