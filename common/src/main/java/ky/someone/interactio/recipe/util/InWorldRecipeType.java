package ky.someone.interactio.recipe.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ky.someone.interactio.recipe.*;
import me.shedaniel.architectury.core.AbstractRecipeSerializer;
import me.shedaniel.architectury.registry.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static ky.someone.interactio.Interactio.*;
import static net.minecraft.core.Registry.RECIPE_SERIALIZER_REGISTRY;
import static net.minecraft.core.Registry.RECIPE_TYPE;

public class InWorldRecipeType<T extends InWorldRecipe<?, ?, ?>> implements RecipeType<T> {

    //private static final Registry<RecipeType<?>> TYPES = REGISTRIES.get(RECIPE_TYPE_REGISTRY);
    private static final Registry<RecipeSerializer<?>> SERIALIZERS = REGISTRIES.get(RECIPE_SERIALIZER_REGISTRY);

    private static final Collection<InWorldRecipeType<?>> types = new HashSet<>();

    public static final InWorldRecipeType<ItemFluidTransformRecipe> ITEM_FLUID_TRANSFORM = create("item_fluid_transform", ItemFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<FluidFluidTransformRecipe> FLUID_FLUID_TRANSFORM = create("fluid_fluid_transform", FluidFluidTransformRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemExplosionRecipe> ITEM_EXPLODE = create("item_explode", ItemExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockExplosionRecipe> BLOCK_EXPLODE = create("block_explode", BlockExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemLightningRecipe> ITEM_LIGHTNING = create("item_lightning", ItemLightningRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemAnvilSmashingRecipe> ITEM_ANVIL_SMASHING = create("item_anvil_smashing", ItemAnvilSmashingRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockAnvilSmashingRecipe> BLOCK_ANVIL_SMASHING = create("block_anvil_smashing", BlockAnvilSmashingRecipe.SERIALIZER);

    private static <T extends InWorldRecipe<?, ?, ?>> InWorldRecipeType<T> create(String name, AbstractRecipeSerializer<T> serializer) {
        return new InWorldRecipeType<>(name, serializer);
    }

    public static void init() {
        types.forEach(type -> {
            // TODO: yell at dan or wait for forge to move recipe types to forge registries
            net.minecraft.core.Registry.register(RECIPE_TYPE, type.registryName, type);
            SERIALIZERS.register(type.registryName, () -> type.serializer);
        });
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
    public final AbstractRecipeSerializer<T> serializer;

    private InWorldRecipeType(String name, AbstractRecipeSerializer<T> serializer) {
        this.registryName = id(name);
        this.serializer = serializer;
        types.add(this);
    }

    @Override
    public String toString() {
        return registryName.toString();
    }

    public List<T> getRecipes() {
        if (cachedRecipes == null) {
            RecipeManager manager = PROXY.getRecipeManager();
            if (manager == null) return Collections.emptyList();
            update(manager);
        }
        return cachedRecipes;
    }

    public Ingredient getValidInputs() {
        if (cachedInputs == null) {
            RecipeManager manager = PROXY.getRecipeManager();
            if (manager == null) return Ingredient.EMPTY;
            update(manager);

        }
        return cachedInputs;
    }

    public void update(RecipeManager manager) {
        ImmutableList.Builder<T> recipes = ImmutableList.builder();
        ImmutableSet.Builder<ItemStack> inputs = ImmutableSet.builder();

        manager.getAllRecipesFor(this).forEach(r -> {
            recipes.add(r);
            r.getIngredients().forEach(i -> inputs.add(i.getItems()));
        });

        cachedRecipes = recipes.build();
        Set<ItemStack> items = inputs.build();
        LOGGER.info("{} - {}", this, items);
        cachedInputs = Ingredient.of(items.stream());

        LOGGER.debug("Fetched {} recipes for type {}!", cachedRecipes.size(), this);
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
