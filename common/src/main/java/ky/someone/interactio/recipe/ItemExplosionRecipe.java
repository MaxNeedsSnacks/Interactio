package ky.someone.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import ky.someone.interactio.Utils;
import ky.someone.interactio.event.ExplosionHandler;
import ky.someone.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.interactio.recipe.util.EntrySerializer;
import ky.someone.interactio.recipe.util.InWorldRecipe;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import me.shedaniel.architectury.core.AbstractRecipeSerializer;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static ky.someone.interactio.Utils.compareStacks;
import static ky.someone.interactio.Utils.sendParticle;

public final class ItemExplosionRecipe implements InWorldRecipe.ItemsStateless<ExplosionHandler.ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<ItemStack> output;
    private final List<RecipeIngredient> inputs;

    public ItemExplosionRecipe(ResourceLocation id, WeightedOutput<ItemStack> output, List<RecipeIngredient> inputs) {
        this.id = id;
        this.output = output;
        this.inputs = inputs;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return Utils.compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionHandler.ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        Level level = info.getLevel();
        Random rand = level.random;

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        // FIXME: this is unreliable
        Vec3 pos = explosion.getDamageSource().getSourcePosition();

        while (Utils.compareStacks(loopingEntities, used, inputs)) {
            // shrink and update items, protecting them from the explosion
            loopingEntities.forEach(e -> e.setInvulnerable(true));
            Utils.shrinkAndUpdate(used);

            double x = pos.x() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.y() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.z() + Mth.nextDouble(rand, 0.25, 0.75);

            double vel = Mth.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(level, x, y, z, stack.copy());
                newItem.setDeltaMovement(0, vel, 0);
                newItem.setPickUpDelay(20);
                level.addFreshEntity(newItem);
            });

            Utils.sendParticle(ParticleTypes.END_ROD, level, pos);

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
    public AbstractRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.ITEM_EXPLODE;
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

    public static class Serializer extends AbstractRecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
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

            return new ItemExplosionRecipe(id, output, inputs);
        }

        @Nullable
        @Override
        public ItemExplosionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.fromNetwork(buffer, EntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.fromNetwork(buffer);
                inputs.add(stack);
            }

            return new ItemExplosionRecipe(id, output, inputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemExplosionRecipe recipe) {
            recipe.output.toNetwork(buffer, EntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.toNetwork(buffer));
        }
    }

}
