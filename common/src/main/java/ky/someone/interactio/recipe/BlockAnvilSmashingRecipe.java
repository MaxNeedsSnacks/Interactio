package ky.someone.interactio.recipe;

import com.google.gson.JsonObject;
import ky.someone.interactio.Utils;
import ky.someone.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.interactio.recipe.ingredient.BlockOrItemOutput;
import ky.someone.interactio.recipe.util.InWorldRecipe;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import me.shedaniel.architectury.core.AbstractRecipeSerializer;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

import static ky.someone.interactio.Utils.sendParticle;

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
        Level level = info.getWorld();
        Random rand = level.random;
        BlockPos hitPos = info.getPos();

        // set it to the default state of our resulting block
        if (output.isBlock()) {
            Block block = output.getBlock();
            if (block != null) level.setBlockAndUpdate(hitPos, block.defaultBlockState());
        } else if (output.isItem()) {
            level.destroyBlock(hitPos, false);
            Collection<ItemStack> stacks = output.getItems();
            if (stacks != null) {
                double x = hitPos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
                double y = hitPos.getY() + Mth.nextDouble(rand, 0.5, 1);
                double z = hitPos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

                double vel = Mth.nextDouble(rand, 0.1, 0.25);

                stacks.forEach(stack -> {
                    ItemEntity newItem = new ItemEntity(level, x, y, z, stack.copy());
                    newItem.setDeltaMovement(0, vel, 0);
                    newItem.setPickUpDelay(20);
                    level.addFreshEntity(newItem);
                });
            }
        }

        // damage anvil
        if (rand.nextDouble() < damage) {
            Utils.sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(anvilPos)), level, Vec3.atCenterOf(anvilPos), 25);
            BlockState dmg = AnvilBlock.damage(level.getBlockState(anvilPos));
            if (dmg == null) {
                level.setBlockAndUpdate(anvilPos, Blocks.AIR.defaultBlockState());
                level.levelEvent(1029, anvilPos, 0);
            } else {
                level.setBlockAndUpdate(anvilPos, dmg);
            }
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public AbstractRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public InWorldRecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_ANVIL_SMASHING;
    }

    public BlockOrItemOutput getOutput() {
        return this.output;
    }

    public BlockIngredient getInput() {
        return this.input;
    }

    private static class Serializer extends AbstractRecipeSerializer<BlockAnvilSmashingRecipe> {
        @Override
        public BlockAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            BlockOrItemOutput output = BlockOrItemOutput.fromJson(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            double damage = Utils.parseChance(json, "damage");

            return new BlockAnvilSmashingRecipe(id, output, input, damage);
        }

        @Nullable
        @Override
        public BlockAnvilSmashingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            BlockOrItemOutput output = BlockOrItemOutput.fromNetwork(buffer);
            BlockIngredient input = BlockIngredient.fromNetwork(buffer);

            double damage = buffer.readDouble();

            return new BlockAnvilSmashingRecipe(id, output, input, damage);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BlockAnvilSmashingRecipe recipe) {
            recipe.output.toNetwork(buffer);
            recipe.input.toNetwork(buffer);

            buffer.writeDouble(recipe.damage);
        }
    }

}
