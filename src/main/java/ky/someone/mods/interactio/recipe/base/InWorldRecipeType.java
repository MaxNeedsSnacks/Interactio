package ky.someone.mods.interactio.recipe.base;

import static ky.someone.mods.interactio.Interactio.LOGGER;
import static ky.someone.mods.interactio.Interactio.MOD_BUS;
import static ky.someone.mods.interactio.Interactio.MOD_ID;
import static ky.someone.mods.interactio.Interactio.PROXY;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.recipe.BlockAnvilSmashingRecipe;
import ky.someone.mods.interactio.recipe.BlockEntityKillRecipe;
import ky.someone.mods.interactio.recipe.BlockExplosionRecipe;
import ky.someone.mods.interactio.recipe.BlockLightningRecipe;
import ky.someone.mods.interactio.recipe.ItemAnvilSmashingRecipe;
import ky.someone.mods.interactio.recipe.ItemEntityKillRecipe;
import ky.someone.mods.interactio.recipe.ItemExplosionRecipe;
import ky.someone.mods.interactio.recipe.ItemFireRecipe;
import ky.someone.mods.interactio.recipe.ItemFluidRecipe;
import ky.someone.mods.interactio.recipe.ItemLightningRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InWorldRecipeType<T extends InWorldRecipe<?, ?, ?>> implements RecipeType<T> {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    private static final Collection<InWorldRecipeType<?>> types = new HashSet<>();

    public static final InWorldRecipeType<ItemFireRecipe> ITEM_BURN = create("item_burning", ItemFireRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemFluidRecipe> ITEM_FLUID = create("item_fluid", ItemFluidRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemExplosionRecipe> ITEM_EXPLODE = create("item_explode", ItemExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockExplosionRecipe> BLOCK_EXPLODE = create("block_explode", BlockExplosionRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemLightningRecipe> ITEM_LIGHTNING = create("item_lightning", ItemLightningRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockLightningRecipe> BLOCK_LIGHTNING = create("block_lightning", BlockLightningRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemAnvilSmashingRecipe> ITEM_ANVIL = create("item_anvil", ItemAnvilSmashingRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockAnvilSmashingRecipe> BLOCK_ANVIL = create("block_anvil", BlockAnvilSmashingRecipe.SERIALIZER);
    public static final InWorldRecipeType<ItemEntityKillRecipe> ITEM_ENTITY_KILL = create("item_entity_kill", ItemEntityKillRecipe.SERIALIZER);
    public static final InWorldRecipeType<BlockEntityKillRecipe> BLOCK_ENTITY_KILL = create("block_entity_kill", BlockEntityKillRecipe.SERIALIZER);

    private static <T extends InWorldRecipe<?, ?, ?>> InWorldRecipeType<T> create(String name, RecipeSerializer<T> serializer) {
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
    public final RecipeSerializer<T> serializer;

    private InWorldRecipeType(String name, RecipeSerializer<T> serializer) {
        this.registryName = Interactio.id(name);
        this.serializer = serializer;
        SERIALIZERS.register(name, () -> serializer);
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

            cachedRecipes = ImmutableList.copyOf(manager.getAllRecipesFor(this));
            LOGGER.debug("Fetched {} recipes for type {}!", cachedRecipes.size(), this);
        }
        return cachedRecipes;
    }

    public Ingredient getValidInputs() {
        if (cachedInputs == null) {
            cachedInputs = Ingredient.merge(
                    stream()
                            .map(Recipe::getIngredients)
                            .flatMap(NonNullList::stream)
                            .collect(Collectors.toSet())
            );
            LOGGER.debug("Fetched all valid recipe inputs for type {}!", this);
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
