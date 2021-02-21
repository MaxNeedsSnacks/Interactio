package ky.someone.mods.interactio.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.sendParticle;

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
        if (!fluid.test(state.getType()) || !state.isSource()) return false;
        if (consumeFluid > 0 && !state.isSource()) return false;

        return compareStacks(entities, inputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        if (compareStacks(entities, used, inputs)) {

            // shrink and update items
            Utils.shrinkAndUpdate(used);

            // consume block if set
            if (world.random.nextDouble() < consumeFluid) {
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }

            // spawn recipe output as item entity
            Random rand = world.random;
            double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

            double vel = Mth.nextDouble(rand, 0.1, 0.25);

            Collection<ItemStack> stacks = output.roll();
            stacks.forEach(stack -> {
                ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                newItem.setDeltaMovement(0, vel, 0);
                newItem.setPickUpDelay(20);
                world.addFreshEntity(newItem);
            });

            // spawn fancy(TM) particles
            sendParticle(ParticleTypes.END_ROD, world, new Vec3(x, y, z));
        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, inputs.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public FluidIngredient getFluid() {
        return fluid;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
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

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<ItemFluidTransformRecipe> {
        @Override
        public ItemFluidTransformRecipe fromJson(ResourceLocation id, JsonObject json) {
            WeightedOutput<ItemStack> output = Utils.singleOrWeighted(GsonHelper.getAsJsonObject(json, "output"), IEntrySerializer.ITEM);
            FluidIngredient fluid = FluidIngredient.deserialize(json.get("fluid"));

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

            double consumeFluid = Utils.parseChance(json, "consume_fluid");

            return new ItemFluidTransformRecipe(id, output, fluid, inputs, consumeFluid);
        }

        @Nullable
        @Override
        public ItemFluidTransformRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
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
        public void toNetwork(FriendlyByteBuf buffer, ItemFluidTransformRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.ITEM);
            recipe.fluid.write(buffer);

            buffer.writeVarInt(recipe.inputs.size());
            recipe.inputs.forEach(stack -> stack.write(buffer));

            buffer.writeDouble(recipe.consumeFluid);
        }
    }

}
