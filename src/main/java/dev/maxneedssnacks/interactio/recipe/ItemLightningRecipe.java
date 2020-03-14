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
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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

public final class ItemLightningRecipe implements InWorldRecipe.ItemsStateless<DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<ItemStack> output;
    private final List<RecipeIngredient> inputs;
    private final double chance;

    public ItemLightningRecipe(ResourceLocation id, WeightedOutput<ItemStack> output, List<RecipeIngredient> inputs, double chance) {
        this.id = id;
        this.output = output;
        this.inputs = inputs;
        this.chance = chance;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        World world = info.getWorld();
        BlockPos pos = info.getPos();
        Random rand = world.rand;

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        while (compareStacks(loopingEntities, used, inputs)) {
            // shrink and update items, protecting them from the explosion
            loopingEntities.forEach(e -> e.setInvulnerable(true));
            Utils.shrinkAndUpdate(used);

            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

            double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                newItem.setMotion(0, vel, 0);
                newItem.setPickupDelay(20);
                newItem.setInvulnerable(true);
                world.addEntity(newItem);
            });

            sendParticle(ParticleTypes.END_ROD, world, new Vec3d(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }


        // set any leftover entities to be vulnerable again
        loopingEntities.forEach(e -> e.setInvulnerable(false));

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
        return InWorldRecipeType.ITEM_LIGHTNING;
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

    public double getChance() {
        return this.chance;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemLightningRecipe> {
        @Override
        public ItemLightningRecipe read(ResourceLocation id, JsonObject json) {
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

            double chance = Utils.parseChance(json, "chance", 1);

            return new ItemLightningRecipe(id, output, inputs, chance);
        }

        @Nullable
        @Override
        public ItemLightningRecipe read(ResourceLocation id, PacketBuffer buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.read(buffer, IEntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.read(buffer);
                inputs.add(stack);
            }

            double chance = buffer.readDouble();

            return new ItemLightningRecipe(id, output, inputs, chance);
        }

        @Override
        public void write(PacketBuffer buffer, ItemLightningRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.write(buffer));

            buffer.writeDouble(recipe.chance);
        }
    }

}
