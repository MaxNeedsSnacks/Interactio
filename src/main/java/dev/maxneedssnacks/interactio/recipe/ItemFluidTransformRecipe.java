package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.FluidIngredient;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import dev.maxneedssnacks.interactio.recipe.util.IngredientStack;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticlePacket;

@Value
public final class ItemFluidTransformRecipe implements InWorldRecipe.ItemsInFluid {

    public static final Serializer SERIALIZER = new Serializer();

    ResourceLocation id;

    ItemStack result;
    FluidIngredient fluid;
    List<IngredientStack> inputs;
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

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        if (compareStacks(entities, used, inputs)) {

            // shrink and update items
            Utils.shrinkAndUpdate(used);

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
            newItem.setMotion(0, vel, 0);
            newItem.setPickupDelay(20);
            world.addEntity(newItem);

            // spawn fancy(TM) particles
            sendParticlePacket(world, newItem);
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, inputs.stream().map(IngredientStack::getIngredient).toArray(Ingredient[]::new));
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
        return InWorldRecipeType.ITEM_FLUID_TRANSFORM;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemFluidTransformRecipe> {
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation id, JsonObject json) {
            ItemStack result = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

            FluidIngredient fluid = FluidIngredient.deserialize(json.get("fluid"));

            List<IngredientStack> inputs = new ArrayList<>();
            JSONUtils.getJsonArray(json, "inputs").forEach(input -> {
                IngredientStack stack = IngredientStack.deserialize(input);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    inputs.add(stack);
                }
            });
            if (inputs.isEmpty()) {
                throw new JsonParseException(String.format("No valid inputs specified for recipe %s!", id));
            }

            double consume = Utils.parseChance(json, "consume");

            return new ItemFluidTransformRecipe(id, result, fluid, inputs, consume);
        }

        @Nullable
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation id, PacketBuffer buffer) {
            ItemStack result = buffer.readItemStack();
            FluidIngredient fluid = FluidIngredient.read(buffer);

            List<IngredientStack> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                IngredientStack stack = IngredientStack.read(buffer);
                inputs.add(stack);
            }

            double consume = buffer.readDouble();

            return new ItemFluidTransformRecipe(id, result, fluid, inputs, consume);
        }

        @Override
        public void write(PacketBuffer buffer, ItemFluidTransformRecipe recipe) {
            buffer.writeItemStack(recipe.result);

            recipe.fluid.write(buffer);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(stack -> stack.write(buffer));

            buffer.writeDouble(recipe.consume);
        }
    }

}
