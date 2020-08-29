package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ingredient.FluidIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.RecipeIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
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

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticle;

public final class FluidFluidTransformRecipe implements InWorldRecipe.ItemsInFluid {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<Fluid> output;
    private final FluidIngredient input;
    private final List<RecipeIngredient> items;

    public FluidFluidTransformRecipe(ResourceLocation id, WeightedOutput<Fluid> output, FluidIngredient input, List<RecipeIngredient> items) {
        this.id = id;
        this.output = output;
        this.input = input;
        this.items = items;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, IFluidState state) {
        if (!input.test(state.getFluid()) || !state.isSource()) return false;
        return compareStacks(entities, items);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        World world = info.getWorld();
        BlockPos pos = info.getPos();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        if (compareStacks(entities, used, items)) {

            Utils.shrinkAndUpdate(used);

            // set block state at position to output
            Fluid fluid = output.rollOnce(); // we only need one block state, so just ignore unique and rolls here.
            if (fluid != null) {
                world.setBlockState(pos, fluid.getDefaultState().getBlockState());
            } else {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            // spawn fancy(TM) particles(?)
            Random rand = world.rand;
            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

            sendParticle(ParticleTypes.END_ROD, world, new Vec3d(x, y, z));

        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, items.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public FluidIngredient getFluid() {
        return input;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return InWorldRecipeType.FLUID_FLUID_TRANSFORM;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public WeightedOutput<Fluid> getOutput() {
        return this.output;
    }

    public FluidIngredient getInput() {
        return this.input;
    }

    public List<RecipeIngredient> getItems() {
        return this.items;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<FluidFluidTransformRecipe> {
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation id, JsonObject json) {
            WeightedOutput<Fluid> output = Utils.singleOrWeighted(JSONUtils.getJsonObject(json, "output"), IEntrySerializer.FLUID);
            FluidIngredient input = FluidIngredient.deserialize(json.get("input"));

            List<RecipeIngredient> items = new ArrayList<>();
            JSONUtils.getJsonArray(json, "items").forEach(item -> {
                RecipeIngredient stack = RecipeIngredient.deserialize(item);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    items.add(stack);
                }
            });
            if (items.isEmpty()) {
                throw new JsonParseException(String.format("No valid items specified for recipe %s!", id));
            }

            return new FluidFluidTransformRecipe(id, output, input, items);
        }

        @Nullable
        @Override
        public FluidFluidTransformRecipe read(ResourceLocation id, PacketBuffer buffer) {
            WeightedOutput<Fluid> output = WeightedOutput.read(buffer, IEntrySerializer.FLUID);
            FluidIngredient input = FluidIngredient.read(buffer);

            List<RecipeIngredient> items = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.read(buffer);
                items.add(stack);
            }

            return new FluidFluidTransformRecipe(id, output, input, items);
        }

        @Override
        public void write(PacketBuffer buffer, FluidFluidTransformRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.FLUID);
            recipe.input.write(buffer);

            buffer.writeVarInt(recipe.items.size());
            recipe.items.forEach(item -> item.write(buffer));
        }
    }

}
