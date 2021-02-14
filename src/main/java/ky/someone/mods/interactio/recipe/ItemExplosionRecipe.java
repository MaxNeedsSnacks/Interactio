package ky.someone.mods.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.sendParticle;

public final class ItemExplosionRecipe implements InWorldRecipe.ItemsStateless<ExplosionInfo> {

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
        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        Level world = info.getWorld();
        Random rand = world.random;

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        Vec3 pos = explosion.getPosition();

        while (compareStacks(loopingEntities, used, inputs)) {
            // shrink and update items, protecting them from the explosion
            loopingEntities.forEach(e -> e.setInvulnerable(true));
            Utils.shrinkAndUpdate(used);

            double x = pos.x() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.y() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.z() + Mth.nextDouble(rand, 0.25, 0.75);

            double vel = Mth.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                newItem.setDeltaMovement(0, vel, 0);
                newItem.setPickUpDelay(20);
                world.addFreshEntity(newItem);
            });

            sendParticle(ParticleTypes.END_ROD, world, pos);

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }


        // set any leftover entities to be vulnerable again
        loopingEntities.forEach(e -> e.setInvulnerable(false));

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

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
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

            return new ItemExplosionRecipe(id, output, inputs);
        }

        @Nullable
        @Override
        public ItemExplosionRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.read(buffer, IEntrySerializer.ITEM);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.read(buffer);
                inputs.add(stack);
            }

            return new ItemExplosionRecipe(id, output, inputs);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemExplosionRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.write(buffer));
        }
    }

}
