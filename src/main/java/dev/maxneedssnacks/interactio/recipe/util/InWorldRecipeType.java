package dev.maxneedssnacks.interactio.recipe.util;

import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.recipe.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.maxneedssnacks.interactio.Interactio.PROXY;

public class InWorldRecipeType<T extends InWorldRecipe<?, ?, ?>> implements IRecipeType<T> {

    private static final Collection<InWorldRecipeType<?>> types = new HashSet<>();

    public static final InWorldRecipeType<ItemFluidTransformRecipe> ITEM_FLUID_TRANSFORM = create("item_fluid_transform", ItemFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<FluidFluidTransformRecipe> FLUID_FLUID_TRANSFORM = create("fluid_fluid_transform", FluidFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemExplosionRecipe> ITEM_EXPLODE = create("item_explode", ItemExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockExplosionRecipe> BLOCK_EXPLODE = create("block_explode", BlockExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemLightningRecipe> ITEM_LIGHTNING = create("item_lightning", ItemLightningRecipe.SERIALIZER);

    private static <T extends InWorldRecipe<?, ?, ?>> InWorldRecipeType<T> create(String name, IRecipeSerializer<T> serializer) {
        InWorldRecipeType<T> type = new InWorldRecipeType<>(name, serializer);
        types.add(type);
        return type;
    }

    public static void registerTypes() {
        types.forEach(type -> {
            Registry.register(Registry.RECIPE_TYPE, type.registryName, type);
            IRecipeSerializer.register(type.registryName.toString(), type.serializer);
        });
    }

    public static void clearCache() {
        types.forEach(type -> {
            type.cachedRecipes.clear();
            type.cachedInputs = null;
        });
    }

    private List<T> cachedRecipes = Collections.emptyList();
    private Ingredient cachedInputs = null;

    public final ResourceLocation registryName;
    public final IRecipeSerializer<T> serializer;

    private InWorldRecipeType(String name, IRecipeSerializer<T> serializer) {
        this.registryName = Interactio.id(name);
        this.serializer = serializer;
        types.add(this);
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    public List<T> getRecipes() {
        RecipeManager manager = PROXY.getRecipeManager();

        if (manager == null) return Collections.emptyList();

        if (cachedRecipes.isEmpty()) {
            //noinspection ConstantConditions
            cachedRecipes = manager.getRecipes(this, null, null);
        }

        return cachedRecipes;
    }

    public Ingredient getValidInputs() {
        if (cachedInputs == null) {
            cachedInputs = Ingredient.merge(
                    stream()
                            .map(IRecipe::getIngredients)
                            .flatMap(NonNullList::stream)
                            .collect(Collectors.toSet())
            );
        }
        return cachedInputs;
    }

    public boolean isValidInput(ItemStack stack) {
        return getValidInputs().test(stack);
    }

    public Stream<T> stream() {
        return getRecipes().stream();
    }

    public Optional<T> findFirst(Predicate<T> predicate) {
        return stream().filter(predicate).findFirst();
    }

    public void apply(Predicate<T> predicate, Consumer<T> callback) {
        findFirst(predicate).ifPresent(callback);
    }

    public void applyAll(Predicate<T> predicate, Consumer<T> callback) {
        stream().filter(predicate).forEach(callback);
    }

}
