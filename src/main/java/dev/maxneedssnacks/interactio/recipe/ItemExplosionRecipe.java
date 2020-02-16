package dev.maxneedssnacks.interactio.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.event.ExplosionHandler.ExplosionInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import dev.maxneedssnacks.interactio.recipe.util.IngredientStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticlePacket;

@Value
public final class ItemExplosionRecipe implements InWorldRecipe.ItemsStateless<ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    ItemStack result;
    List<IngredientStack> inputs;
    double chance;

    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

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

            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

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
        return NonNullList.from(Ingredient.EMPTY, inputs.stream().map(IngredientStack::getIngredient).toArray(Ingredient[]::new));
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
        return InWorldRecipeType.ITEM_EXPLODE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe read(ResourceLocation id, JsonObject json) {
            ItemStack result = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

            List<IngredientStack> inputs = new ArrayList<>();
            JSONUtils.getJsonArray(json, "inputs").forEach(input -> {
                IngredientStack stack = IngredientStack.deserialize(input);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    inputs.add(stack);
                }
            });
            if (inputs.isEmpty()) {
                throw new JsonParseException("No valid inputs specified for item explosion recipe!");
            }

            double chance = Utils.parseChance(json, "chance", 1);

            return new ItemExplosionRecipe(id, result, inputs, chance);
        }

        @Nullable
        @Override
        public ItemExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            ItemStack result = buffer.readItemStack();

            List<IngredientStack> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                IngredientStack stack = IngredientStack.read(buffer);
                inputs.add(stack);
            }

            double chance = buffer.readDouble();

            return new ItemExplosionRecipe(id, result, inputs, chance);
        }

        @Override
        public void write(PacketBuffer buffer, ItemExplosionRecipe recipe) {
            buffer.writeItemStack(recipe.result);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(item -> item.write(buffer));

            buffer.writeDouble(recipe.chance);

        }
    }

}
