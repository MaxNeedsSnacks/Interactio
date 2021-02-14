package ky.someone.mods.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
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
import net.minecraftforge.registries.ForgeRegistryEntry;

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
        Level world = info.getWorld();
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
                sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, world.getBlockState(pos)), world, Vec3.atBottomCenterOf(pos), 25);
                BlockState dmg = AnvilBlock.damage(world.getBlockState(pos));
                if (dmg == null) {
                    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    world.levelEvent(1029, pos, 0);
                    anvilBroke = true;
                } else {
                    world.setBlockAndUpdate(pos, dmg);
                }
            }

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> Block.popResource(world, pos, stack.copy()));

            sendParticle(ParticleTypes.END_ROD, world, Vec3.atBottomCenterOf(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, inputs.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
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

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemAnvilSmashingRecipe> {
        @Override
        public ItemAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            WeightedOutput<ItemStack> output = Utils.singleOrWeighted(GsonHelper.getAsJsonObject(json, "output"), IEntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, "inputs").forEach(input -> {
                RecipeIngredient stack = RecipeIngredient.deserialize(input);
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
        @Override
        public ItemAnvilSmashingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
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
        public void toNetwork(FriendlyByteBuf buffer, ItemAnvilSmashingRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.write(buffer));

            buffer.writeDouble(recipe.damage);
        }
    }
}
