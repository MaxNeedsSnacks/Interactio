package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.maxneedssnacks.interactio.Interactio;
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
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static dev.maxneedssnacks.interactio.Utils.compareStacks;
import static dev.maxneedssnacks.interactio.Utils.sendParticle;

public final class ItemFluidTransformRecipe implements InWorldRecipe.ItemsInFluid {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final WeightedOutput<ItemStack> output;
    private final FluidIngredient fluid;
    private final List<RecipeIngredient> inputs;
    private final double consumeFluid;

    public ItemFluidTransformRecipe(ResourceLocation id, WeightedOutput<ItemStack> output, FluidIngredient fluid, List<RecipeIngredient> inputs, double consumeFluid) {
        this.id = id;
        this.output = output;
        this.fluid = fluid;
        this.inputs = inputs;
        this.consumeFluid = consumeFluid;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, FluidState state) {
        Interactio.LOGGER.info(entities);
        Interactio.LOGGER.info(state);

        if (!fluid.test(state.getFluid()) || !state.isSource()) return false;
        if (consumeFluid > 0 && !state.isSource()) return false;

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
            if (world.rand.nextDouble() < consumeFluid) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState());
            }

            // spawn recipe output as item entity
            Random rand = world.rand;
            double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

            double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                newItem.setMotion(0, vel, 0);
                newItem.setPickupDelay(20);
                world.addEntity(newItem);
            });

            // spawn fancy(TM) particles
            sendParticle(ParticleTypes.END_ROD, world, new Vector3d(x, y, z));
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.from(Ingredient.EMPTY, inputs.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public FluidIngredient getFluid() {
        return fluid;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return InWorldRecipeType.ITEM_FLUID_TRANSFORM;
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

    public double getConsumeFluid() {
        return this.consumeFluid;
    }

    public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ItemFluidTransformRecipe> {
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation id, JsonObject json) {
            WeightedOutput<ItemStack> output = Utils.singleOrWeighted(JSONUtils.getJsonObject(json, "output"), IEntrySerializer.ITEM);
            FluidIngredient fluid = FluidIngredient.deserialize(json.get("fluid"));

            List<RecipeIngredient> inputs = new ArrayList<>();
            JSONUtils.getJsonArray(json, "inputs").forEach(input -> {
                RecipeIngredient stack = RecipeIngredient.deserialize(input);
                if (!stack.getIngredient().hasNoMatchingItems()) {
                    inputs.add(stack);
                }
            });
            if (inputs.isEmpty()) {
                throw new JsonParseException(String.format("No valid inputs specified for recipe %s!", id));
            }

            double consumeFluid = Utils.parseChance(json, "consume_fluid");

            return new ItemFluidTransformRecipe(id, output, fluid, inputs, consumeFluid);
        }

        @Nullable
        @Override
        public ItemFluidTransformRecipe read(ResourceLocation id, PacketBuffer buffer) {
            WeightedOutput<ItemStack> output = WeightedOutput.read(buffer, IEntrySerializer.ITEM);
            FluidIngredient fluid = FluidIngredient.read(buffer);

            List<RecipeIngredient> inputs = new ArrayList<>();
            int ingrCount = buffer.readVarInt();
            for (int i = 0; i < ingrCount; ++i) {
                RecipeIngredient stack = RecipeIngredient.read(buffer);
                inputs.add(stack);
            }

            double consumeFluid = buffer.readDouble();

            return new ItemFluidTransformRecipe(id, output, fluid, inputs, consumeFluid);
        }

        @Override
        public void write(PacketBuffer buffer, ItemFluidTransformRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);
            recipe.fluid.write(buffer);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(stack -> stack.write(buffer));

            buffer.writeDouble(recipe.consumeFluid);
        }
    }

}
