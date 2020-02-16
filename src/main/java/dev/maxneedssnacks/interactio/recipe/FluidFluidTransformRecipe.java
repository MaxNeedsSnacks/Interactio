package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.recipe.util.FluidIngredient;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import dev.maxneedssnacks.interactio.recipe.util.IngredientStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Value;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.*;

@Value
public final class FluidFluidTransformRecipe implements InWorldRecipe.ItemsInFluid {

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    Fluid result;
    FluidIngredient input;
    List<IngredientStack> items;
    boolean consumeItems;

    @Override
    public boolean canCraft(List<ItemEntity> entities, IFluidState state) {
        if (!input.test(state.getFluid()) || !state.isSource()) return false;
        return compareStacks(entities, items);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {

        World world = info.getWorld();
        BlockPos pos = info.getPos();

        if (!canCraft(entities, world.getFluidState(pos))) {
            throw new IllegalStateException("Attempted to perform illegal craft on fluid transform recipe!");
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
            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

            sendParticlePacket(world, new Vec3d(x, y, z));

        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, items.stream().map(IngredientStack::getIngredient).toArray(Ingredient[]::new));
    }

    public boolean consumesItems() {
        return consumeItems;
    }

    @Override
    public FluidIngredient getFluid() {
        return input;
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
        return InWorldRecipeType.FLUID_FLUID_TRANSFORM;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FluidFluidTransformRecipe> {
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation recipeId, JsonObject json) {

            Fluid result = parseFluidStrict(JSONUtils.getString(json, "result"));

            FluidIngredient input = FluidIngredient.deserialize(json.get("input"));

            List<IngredientStack> items = new ArrayList<>();
            JSONUtils.getJsonArray(json, "items").forEach(item -> {
                IngredientStack stack = IngredientStack.deserialize(item);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    items.add(stack);
                }
            });
            if (items.isEmpty()) {
                throw new JsonParseException("No valid items specified for fluid transform recipe!");
            }

            boolean consumeItems = JSONUtils.getBoolean(json, "consume_items", false);

            return new FluidFluidTransformRecipe(recipeId, result, input, items, consumeItems);
        }

        @Nullable
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {

            Fluid result = parseFluidStrict(buffer.readResourceLocation());
            FluidIngredient input = FluidIngredient.read(buffer);

            List<IngredientStack> items = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                IngredientStack stack = IngredientStack.read(buffer);
                items.add(stack);
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
            recipe.items.forEach(item -> item.write(buffer));

            buffer.writeBoolean(recipe.consumeItems);

        }
    }

}
