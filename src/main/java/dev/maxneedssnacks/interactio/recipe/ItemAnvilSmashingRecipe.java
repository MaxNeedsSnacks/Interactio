package dev.maxneedssnacks.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ingredient.RecipeIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe.DefaultInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticle;

public final class ItemAnvilSmashingRecipe implements InWorldRecipe<List<ItemEntity>, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<ItemStack> output;
    private final List<RecipeIngredient> inputs;

    private final double damage;

    public ItemAnvilSmashingRecipe(ResourceLocation id, WeightedOutput<ItemStack> output, List<RecipeIngredient> inputs, double damage) {
        this.id = id;
        this.output = output;
        this.inputs = inputs;
        this.damage = damage;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, BlockState state) {
        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        World world = info.getWorld();
        BlockPos pos = info.getPos();
        Random rand = world.getRandom();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        boolean anvilBroke = false;
        while (compareStacks(loopingEntities, used, inputs) && !anvilBroke) {

            // shrink and update items
            Utils.shrinkAndUpdate(used);

            // damage anvil
            if (rand.nextDouble() < damage) {
                sendParticle(new BlockParticleData(ParticleTypes.BLOCK, world.getBlockState(pos)), world, new Vec3d(pos), 25);
                BlockState dmg = AnvilBlock.damage(world.getBlockState(pos));
                if (dmg == null) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    world.playEvent(1029, pos, 0);
                    anvilBroke = true;
                } else {
                    world.setBlockState(pos, dmg);
                }
            }

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> Block.spawnAsEntity(world, pos, stack.copy()));

            sendParticle(ParticleTypes.END_ROD, world, new Vec3d(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, inputs.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return InWorldRecipeType.ITEM_ANVIL_SMASHING;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public WeightedOutput<ItemStack> getOutput() {
        return this.output;
    }

    public List<RecipeIngredient> getInputs() {
        return this.inputs;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemAnvilSmashingRecipe> {
        @Override
        public ItemAnvilSmashingRecipe read(ResourceLocation id, JsonObject json) {
            WeightedOutput<ItemStack> output = Utils.singleOrWeighted(JSONUtils.getJsonObject(json, "output"), IEntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            JSONUtils.getJsonArray(json, "inputs").forEach(input -> {
                RecipeIngredient stack = RecipeIngredient.deserialize(input);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    inputs.add(stack);
                }
            });
            if (inputs.isEmpty()) {
                throw new JsonParseException(String.format("No valid inputs specified for recipe %s!", id));
            }

            double damage = Utils.parseChance(json, "damage");

            return new ItemAnvilSmashingRecipe(id, output, inputs, damage);
        }

        @Nullable
        @Override
        public ItemAnvilSmashingRecipe read(ResourceLocation id, PacketBuffer buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.read(buffer, IEntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.read(buffer);
                inputs.add(stack);
            }

            double damage = buffer.readDouble();

            return new ItemAnvilSmashingRecipe(id, output, inputs, damage);
        }

        @Override
        public void write(PacketBuffer buffer, ItemAnvilSmashingRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.write(buffer));

            buffer.writeDouble(recipe.damage);
        }
    }
}
