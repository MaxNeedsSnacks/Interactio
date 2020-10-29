package dev.maxneedssnacks.interactio.recipe.util;

import com.google.common.collect.ImmutableList;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.recipe.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.maxneedssnacks.interactio.Interactio.*;

public class InWorldRecipeType<T extends InWorldRecipe<?, ?, ?>> implements IRecipeType<T> {

    private static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    private static final Collection<InWorldRecipeType<?>> types = new HashSet<>();

    public static final InWorldRecipeType<ItemFluidTransformRecipe> ITEM_FLUID_TRANSFORM = create("item_fluid_transform", ItemFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<FluidFluidTransformRecipe> FLUID_FLUID_TRANSFORM = create("fluid_fluid_transform", FluidFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemExplosionRecipe> ITEM_EXPLODE = create("item_explode", ItemExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockExplosionRecipe> BLOCK_EXPLODE = create("block_explode", BlockExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemLightningRecipe> ITEM_LIGHTNING = create("item_lightning", ItemLightningRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemAnvilSmashingRecipe> ITEM_ANVIL_SMASHING = create("item_anvil_smashing", ItemAnvilSmashingRecipe.SERIALIZER);

    private static <T extends InWorldRecipe<?, ?, ?>> InWorldRecipeType<T> create(String name, IRecipeSerializer<T> serializer) {
        return new InWorldRecipeType<>(name, serializer);
    }

    public static void init() {
        types.forEach(type -> Registry.register(Registry.RECIPE_TYPE, type.registryName, type));
        SERIALIZERS.register(MOD_BUS);
    }

    public static void clearCache() {
        types.forEach(type -> {
            type.cachedRecipes = null;
            type.cachedInputs = null;
        });
    }

    private List<T> cachedRecipes = null;
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

    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    public List<T> getRecipes() {
        if (cachedRecipes == null) {
            RecipeManager manager = PROXY.getRecipeManager();
            if (manager == null) return Collections.emptyList();

            cachedRecipes = manager.getRecipes(this)
                    .values()
                    .stream()
                    // we can do this since we're filtering by our recipe type
                    // unless of course, someone does something horrible with the map,
                    // in which case, shame on them, not on us
                    .map(it -> (T) it)
                    .collect(ImmutableList.toImmutableList());
            LOGGER.debug("Fetched {} recipes for type {}!", cachedRecipes.size(), this);
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
            LOGGER.debug("Fetched {} recipe inputs for type {}!", cachedRecipes.size(), this);
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
