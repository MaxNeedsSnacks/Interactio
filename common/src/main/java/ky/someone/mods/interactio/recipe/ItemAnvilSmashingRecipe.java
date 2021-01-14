package ky.someone.mods.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.EntrySerializer;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.sendParticle;

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
        Level level = info.getWorld();
        BlockPos pos = info.getPos();
        Random rand = level.getRandom();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        boolean anvilBroke = false;
        while (compareStacks(loopingEntities, used, inputs) && !anvilBroke) {

            // shrink and update items
            Utils.shrinkAndUpdate(used);

            // damage anvil
            if (rand.nextDouble() < damage) {
                sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(pos)), level, Vec3.atCenterOf(pos), 25);
                BlockState dmg = AnvilBlock.damage(level.getBlockState(pos));
                if (dmg == null) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.levelEvent(1029, pos, 0);
                    anvilBroke = true;
                } else {
                    level.setBlockAndUpdate(pos, dmg);
                }
            }

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> Block.popResource(level, pos, stack.copy()));

            sendParticle(ParticleTypes.END_ROD, level, Vec3.atCenterOf(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return Utils.nonNullListOf(inputs.stream().map(RecipeIngredient::getIngredient));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
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

    public static class Serializer implements RecipeSerializer<ItemAnvilSmashingRecipe> {
        @Override
        public ItemAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            WeightedOutput<ItemStack> output = Utils.singleOrWeighted(GsonHelper.getAsJsonObject(json, "output"), EntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, "inputs").forEach(input -> {
                RecipeIngredient stack = RecipeIngredient.fromJson(input);
                if (!stack.getIngredient().isEmpty()) {
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
        public ItemAnvilSmashingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.fromNetwork(buffer, EntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.fromNetwork(buffer);
                inputs.add(stack);
            }

            double damage = buffer.readDouble();

            return new ItemAnvilSmashingRecipe(id, output, inputs, damage);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemAnvilSmashingRecipe recipe) {
            recipe.output.toNetwork(buffer, EntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.toNetwork(buffer));

            buffer.writeDouble(recipe.damage);
        }
    }
}
