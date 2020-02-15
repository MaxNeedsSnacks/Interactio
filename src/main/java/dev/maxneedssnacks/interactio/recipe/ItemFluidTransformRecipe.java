package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.FluidIngredient;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import lombok.Value;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticlePacket;

@Value
public final class ItemFluidTransformRecipe implements InWorldRecipe.ItemsInFluid {

    public static final IRecipeType<ItemFluidTransformRecipe> RECIPE_TYPE = new IRecipeType<ItemFluidTransformRecipe>() {
        @Override
        public String toString() {
            return ModRecipes.ITEM_FLUID_TRANSFORM.toString();
        }
    };

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    ItemStack result;
    FluidIngredient fluid;
    Object2IntLinkedOpenHashMap<Ingredient> inputs;
    double consume;

    @Override
    public boolean canCraft(List<ItemEntity> entities, IFluidState state) {

        if (!fluid.test(state.getFluid())) return false;
        if (consume > 0 && !state.isSource()) return false;

        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {

        World world = info.getWorld();
        BlockPos pos = info.getPos();

        if (!canCraft(entities, world.getFluidState(pos))) {
            throw new IllegalStateException("Attempted to perform illegal craft on item fluid transform recipe!");
        }

        Object2IntLinkedOpenHashMap<ItemEntity> used = new Object2IntLinkedOpenHashMap<>();

        if (compareStacks(entities, used, inputs)) {

            // shrink and update items
            used.forEach((entity, count) -> {
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

            // consume block if set
            if (world.rand.nextDouble() < consume) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            // spawn recipe output as item entity
            Random rand = world.rand;
            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

            double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

            ItemEntity newItem = new ItemEntity(world, x, y, z, getRecipeOutput());
            newItem.setVelocity(0, vel, 0);
            newItem.setPickupDelay(20);
            world.addEntity(newItem);

            // spawn fancy(TM) particles
            sendParticlePacket(world, newItem);
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, inputs.keySet().toArray(new Ingredient[0]));
    }

    public double consumeChance() {
        return consume;
    }

    @Override
    public FluidIngredient getFluid() {
        return fluid;
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

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemFluidTransformRecipe> {
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation recipeId, JsonObject json) {

            ItemStack result = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

            FluidIngredient fluid = FluidIngredient.deserialize(json.get("fluid"));

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

            double consume = Utils.parseChance(json, "consume");

            return new ItemFluidTransformRecipe(recipeId, result, fluid, inputs, consume);
        }

        @Nullable
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {

            ItemStack result = buffer.readItemStack();
            FluidIngredient fluid = FluidIngredient.read(buffer);

            Object2IntLinkedOpenHashMap<Ingredient> inputs = new Object2IntLinkedOpenHashMap<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                Ingredient ingredient = Ingredient.read(buffer);
                int count = buffer.readVarInt();
                inputs.put(ingredient, count);
            }

            double consume = buffer.readDouble();

            return new ItemFluidTransformRecipe(recipeId, result, fluid, inputs, consume);
        }

        @Override
        public void write(PacketBuffer buffer, ItemFluidTransformRecipe recipe) {

            buffer.writeItemStack(recipe.result);

            recipe.fluid.write(buffer);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(((ingredient, count) -> {
                ingredient.write(buffer);
                buffer.writeVarInt(count);
            }));

            buffer.writeDouble(recipe.consume);

        }
    }

}
