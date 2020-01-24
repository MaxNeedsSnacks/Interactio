package dev.maxneedssnacks.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.CraftingInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import lombok.Value;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.*;

@Value
public final class ItemExplosionRecipe implements InWorldRecipe.Items<ItemExplosionRecipe.ExplosionInfo> {

    public static final IRecipeType<ItemFluidTransformRecipe> RECIPE_TYPE = new IRecipeType<ItemFluidTransformRecipe>() {
        @Override
        public String toString() {
            return ModRecipes.ITEM_EXPLODE.toString();
        }
    };

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    ItemStack result;
    Object2IntLinkedOpenHashMap<Ingredient> inputs;
    double chance;

    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();

        Object2IntLinkedOpenHashMap<ItemEntity> used = new Object2IntLinkedOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        int spawnAmt = 0;

        while (compareStacks(loopingEntities, used, inputs)) {
            // shrink and update items
            used.forEach((entity, count) -> {
                entity.setInvulnerable(true);
                entity.setInfinitePickupDelay();

                ItemStack item = entity.getItem().copy();
                item.shrink(count);

                if (item.isEmpty()) {
                    entity.remove();
                } else {
                    entity.setItem(item);
                }

                entity.setDefaultPickupDelay();
            });

            // only actually craft the item if the recipe is successful
            if (chance == 1 || world.rand.nextDouble() < chance) {
                spawnAmt += result.getCount();
            }

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
        }

        Random rand = world.rand;
        Vec3d pos = explosion.getPosition();

        while (spawnAmt > 0) {
            int c = Math.min(spawnAmt, 64);

            ItemStack stack = result.copy();
            stack.setCount(c);

            double x = pos.getX() + randomBetween(0.25, 0.75, rand);
            double y = pos.getY() + randomBetween(0.5, 1, rand);
            double z = pos.getZ() + randomBetween(0.25, 0.75, rand);

            ItemEntity newItem = new ItemEntity(world, x, y, z, stack);
            newItem.setPickupDelay(20);
            world.addEntity(newItem);

            sendParticlePacket(world, newItem);

            spawnAmt -= c;
        }

        // set any leftover entities to be vulnerable again
        loopingEntities.forEach(e -> e.setInvulnerable(false));

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, inputs.keySet().toArray(new Ingredient[0]));
    }

    @Override
    public ItemStack getRecipeOutput() {
        return result.copy();
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return RECIPE_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe read(ResourceLocation id, JsonObject json) {
            ItemStack result = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

            Object2IntLinkedOpenHashMap<Ingredient> inputs = new Object2IntLinkedOpenHashMap<>();
            JSONUtils.getJsonArray(json, "inputs").forEach(input -> {
                Ingredient ingredient = Ingredient.deserialize(input);
                int count = JSONUtils.getInt(input.getAsJsonObject(), "count", 1);
                if (!ingredient.hasNoMatchingItems()) {
                    inputs.put(ingredient, count);
                }
            });
            if (inputs.isEmpty()) {
                throw new JsonParseException("No valid inputs specified for item fluid transform recipe!");
            }

            double chance = Utils.parseChance(json, "chance", 1);

            return new ItemExplosionRecipe(id, result, inputs, chance);
        }

        @Nullable
        @Override
        public ItemExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            ItemStack result = buffer.readItemStack();

            Object2IntLinkedOpenHashMap<Ingredient> inputs = new Object2IntLinkedOpenHashMap<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                Ingredient ingredient = Ingredient.read(buffer);
                int count = buffer.readVarInt();
                inputs.put(ingredient, count);
            }

            double chance = buffer.readDouble();

            return new ItemExplosionRecipe(id, result, inputs, chance);
        }

        @Override
        public void write(PacketBuffer buffer, ItemExplosionRecipe recipe) {
            buffer.writeItemStack(recipe.result);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(((ingredient, count) -> {
                ingredient.write(buffer);
                buffer.writeVarInt(count);
            }));

            buffer.writeDouble(recipe.chance);

        }
    }

    @Value
    public static class ExplosionInfo implements CraftingInfo {
        World world;
        Explosion explosion;
    }
}
