package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
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
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.*;

public class ItemFluidTransformRecipe extends InWorldRecipe.ItemsInFluid {

    public static final IRecipeType<ItemFluidTransformRecipe> RECIPE_TYPE = new IRecipeType<ItemFluidTransformRecipe>() {
        @Override
        public String toString() {
            return ModRecipes.ITEM_FLUID_TRANSFORM.toString();
        }
    };

    public static final Serializer SERIALIZER = new Serializer();

    private final ItemStack result;
    private final Object2IntLinkedOpenHashMap<Ingredient> inputs;
    private final FluidIngredient fluid;
    private final boolean consume;

    public ItemFluidTransformRecipe(ResourceLocation id, ItemStack result, FluidIngredient fluid, Object2IntLinkedOpenHashMap<Ingredient> inputs, boolean consume) {
        super(id);
        this.result = result;
        this.fluid = fluid;
        this.inputs = inputs;
        this.consume = consume;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, IFluidState state) {

        if (!fluid.test(state.getFluid())) return false;
        if (consume && !state.isSource()) return false;

        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, World world, BlockPos pos) {

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
            if (consume) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            // spawn recipe output as item entity
            Random rand = world.rand;
            double x = pos.getX() + randomBetween(0.25, 0.75, rand);
            double y = pos.getY() + randomBetween(0.5, 1, rand);
            double z = pos.getZ() + randomBetween(0.25, 0.75, rand);

            double vel = randomBetween(0.1, 0.25, rand);

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

    public Object2IntLinkedOpenHashMap<Ingredient> getInputs() {
        return inputs;
    }

    public boolean consumesFluid() {
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

            boolean consume = JSONUtils.getBoolean(json, "consume", false);

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

            boolean consume = buffer.readBoolean();

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

            buffer.writeBoolean(recipe.consume);

        }
    }

}
