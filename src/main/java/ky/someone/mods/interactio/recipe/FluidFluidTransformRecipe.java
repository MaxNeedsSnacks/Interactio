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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.sendParticle;

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
    public boolean canCraft(List<ItemEntity> entities, FluidState state) {
        if (!input.test(state.getType()) || !state.isSource()) return false;
        return compareStacks(entities, items);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        if (compareStacks(entities, used, items)) {

            Utils.shrinkAndUpdate(used);

            // set block state at position to output
            Fluid fluid = output.rollOnce(); // we only need one block state, so just ignore unique and rolls here.
            if (fluid != null) {
                world.setBlockAndUpdate(pos, fluid.defaultFluidState().createLegacyBlock());
            } else {
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }

            // spawn fancy(TM) particles(?)
            Random rand = world.random;
            double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

            sendParticle(ParticleTypes.END_ROD, world, new Vec3(x, y, z));

        }

    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, items.stream().map(RecipeIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public FluidIngredient getFluid() {
        return input;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
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

    public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<FluidFluidTransformRecipe> {
        @Override
        public FluidFluidTransformRecipe fromJson(ResourceLocation id, JsonObject json) {
            WeightedOutput<Fluid> output = Utils.singleOrWeighted(GsonHelper.getAsJsonObject(json, "output"), IEntrySerializer.FLUID);
            FluidIngredient input = FluidIngredient.deserialize(json.get("input"));

            List<RecipeIngredient> items = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, "items").forEach(item -> {
                RecipeIngredient stack = RecipeIngredient.deserialize(item);
                if (!stack.getIngredient().isEmpty()) {
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
        public FluidFluidTransformRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
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
        public void toNetwork(FriendlyByteBuf buffer, FluidFluidTransformRecipe recipe) {
            recipe.output.write(buffer, IEntrySerializer.FLUID);
            recipe.input.write(buffer);

            buffer.writeVarInt(recipe.items.size());
            recipe.items.forEach(item -> item.write(buffer));
        }
    }

}
