package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.*;

public class FluidFluidTransformRecipe extends InWorldRecipe.ItemsInFluid {

    public static final IRecipeType<FluidFluidTransformRecipe> RECIPE_TYPE = new IRecipeType<FluidFluidTransformRecipe>() {
        @Override
        public String toString() {
            return ModRecipes.FLUID_FLUID_TRANSFORM.toString();
        }
    };

    public static final Serializer SERIALIZER = new Serializer();

    private final Fluid result;
    private final Object2IntLinkedOpenHashMap<Ingredient> items;
    private final FluidIngredient input;
    private final boolean consumeItems;

    public FluidFluidTransformRecipe(ResourceLocation id, Fluid result, FluidIngredient input, Object2IntLinkedOpenHashMap<Ingredient> items, boolean consumeItems) {
        super(id);
        this.result = result;
        this.input = input;
        this.items = items;
        this.consumeItems = consumeItems;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, IFluidState state) {
        if (!input.test(state.getFluid()) || !state.isSource()) return false;
        return compareStacks(entities, items);
    }

    @Override
    public void craft(List<ItemEntity> entities, World world, BlockPos pos) {

        if (!canCraft(entities, world.getFluidState(pos))) {
            throw new IllegalStateException("Attempted to perform illegal craft on input input transform recipe!");
        }

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        if (compareStacks(entities, used, items)) {

            // shrink and update items if recipe is set to consumeItems
            if (consumeItems) {
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
            }

            // set block state at position to new input
            world.setBlockState(pos, result.getDefaultState().getBlockState());

            // spawn fancy(TM) particles(?)
            Random rand = world.rand;
            double x = pos.getX() + randomBetween(0.25, 0.75, rand);
            double y = pos.getY() + randomBetween(0.5, 1, rand);
            double z = pos.getZ() + randomBetween(0.25, 0.75, rand);

            sendParticlePacket(world, new Vec3d(x, y, z));

        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, items.keySet().toArray(new Ingredient[0]));
    }

    public Object2IntLinkedOpenHashMap<Ingredient> getItems() {
        return items;
    }

    public boolean consumesItems() {
        return consumeItems;
    }

    @Override
    public FluidIngredient getFluid() {
        return input;
    }

    public Fluid getOutputFluid() {
        return result;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return RECIPE_TYPE;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FluidFluidTransformRecipe> {
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation recipeId, JsonObject json) {

            Fluid result = parseFluidStrict(JSONUtils.getString(json, "result"));

            FluidIngredient input = FluidIngredient.deserialize(json.get("input"));

            Object2IntLinkedOpenHashMap<Ingredient> items = new Object2IntLinkedOpenHashMap<>();
            JSONUtils.getJsonArray(json, "items").forEach(item -> {
                Ingredient ingredient = Ingredient.deserialize(item);
                int count = JSONUtils.getInt(item.getAsJsonObject(), "count", 1);
                if (!ingredient.hasNoMatchingItems()) {
                    items.put(ingredient, count);
                }
            });
            if (items.isEmpty()) {
                throw new JsonParseException("No valid items specified for input input transform recipe!");
            }

            boolean consumeItems = JSONUtils.getBoolean(json, "consume_items", false);

            return new FluidFluidTransformRecipe(recipeId, result, input, items, consumeItems);
        }

        @Nullable
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {

            Fluid result = parseFluidStrict(buffer.readResourceLocation());
            FluidIngredient input = FluidIngredient.read(buffer);

            Object2IntLinkedOpenHashMap<Ingredient> items = new Object2IntLinkedOpenHashMap<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                Ingredient ingredient = Ingredient.read(buffer);
                int count = buffer.readVarInt();
                items.put(ingredient, count);
            }

            boolean consumeItems = buffer.readBoolean();

            return new FluidFluidTransformRecipe(recipeId, result, input, items, consumeItems);
        }

        @Override
        public void write(PacketBuffer buffer, FluidFluidTransformRecipe recipe) {

            Fluid fluid = recipe.result;
            if (fluid == null || fluid.getRegistryName() == null) {
                buffer.writeResourceLocation(new ResourceLocation("null"));
            } else {
                buffer.writeResourceLocation(fluid.getRegistryName());
            }

            recipe.input.write(buffer);

            buffer.writeVarInt(recipe.items.size());
            recipe.items.forEach(((ingredient, count) -> {
                ingredient.write(buffer);
                buffer.writeVarInt(count);
            }));

            buffer.writeBoolean(recipe.consumeItems);

        }
    }

}
