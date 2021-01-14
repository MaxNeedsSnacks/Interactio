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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.sendParticle;

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
        Level level = info.getWorld();
        BlockPos pos = info.getPos();
        Random rand = level.random;

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        while (compareStacks(loopingEntities, used, inputs)) {
            // shrink and update items, protecting them from the explosion
            loopingEntities.forEach(e -> e.setInvulnerable(true));
            Utils.shrinkAndUpdate(used);

            double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

            double vel = Mth.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(level, x, y, z, stack.copy());
                newItem.setDeltaMovement(0, vel, 0);
                newItem.setPickUpDelay(20);
                newItem.setInvulnerable(true);
                level.addFreshEntity(newItem);
            });

            sendParticle(ParticleTypes.END_ROD, level, Vec3.atCenterOf(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }


        // set any leftover entities to be vulnerable again
        loopingEntities.forEach(e -> e.setInvulnerable(false));

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

    public static class Serializer implements RecipeSerializer<ItemLightningRecipe> {
        @Override
        public ItemLightningRecipe fromJson(ResourceLocation id, JsonObject json) {
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

            double chance = Utils.parseChance(json, "chance", 1);

            return new ItemLightningRecipe(id, output, inputs, chance);
        }

        @Nullable
        @Override
        public ItemLightningRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.fromNetwork(buffer, EntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.fromNetwork(buffer);
                inputs.add(stack);
            }

            double chance = buffer.readDouble();

            return new ItemLightningRecipe(id, output, inputs, chance);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemLightningRecipe recipe) {
            recipe.output.toNetwork(buffer, EntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.toNetwork(buffer));

            buffer.writeDouble(recipe.chance);
        }
    }

}
