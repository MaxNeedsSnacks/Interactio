package dev.maxneedssnacks.interactio.recipe;

import com.google.common.collect.ArrayListMultimap;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

public final class ModRecipes {

    public static final ResourceLocation FLUID_FLUID_TRANSFORM = Interactio.id("fluid_fluid_transform");
    public static final ResourceLocation ITEM_FLUID_TRANSFORM = Interactio.id("item_fluid_transform");
    public static final ResourceLocation ITEM_EXPLODE = Interactio.id("item_explode");

    public static Ingredient ANY_VALID = Ingredient.EMPTY;
    public static ArrayListMultimap<IRecipeType<?>, InWorldRecipe<?, ?, ?>> RECIPE_MAP = ArrayListMultimap.create();

    public static void init() {
        register(ITEM_FLUID_TRANSFORM, ItemFluidTransformRecipe.RECIPE_TYPE, ItemFluidTransformRecipe.SERIALIZER);
        register(FLUID_FLUID_TRANSFORM, FluidFluidTransformRecipe.RECIPE_TYPE, FluidFluidTransformRecipe.SERIALIZER);
        register(ITEM_EXPLODE, ItemExplosionRecipe.RECIPE_TYPE, ItemExplosionRecipe.SERIALIZER);
    }

    public static void register(ResourceLocation name, IRecipeType<?> type, IRecipeSerializer<?> serializer) {
        register(name, type);
        register(name.toString(), serializer);
    }

    private static void register(ResourceLocation name, IRecipeType<?> type) {
        Registry.register(Registry.RECIPE_TYPE, name, type);
    }

    private static void register(String name, IRecipeSerializer<?> serializer) {
        IRecipeSerializer.register(name, serializer);
    }

}
