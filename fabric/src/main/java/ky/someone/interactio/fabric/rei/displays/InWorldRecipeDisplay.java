package ky.someone.interactio.fabric.rei.displays;

import ky.someone.interactio.recipe.util.InWorldRecipe;
import me.shedaniel.rei.api.RecipeDisplay;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class InWorldRecipeDisplay<T extends InWorldRecipe<?, ?, ?>> implements RecipeDisplay {

    protected final T recipe;

    public InWorldRecipeDisplay(T recipe) {
        this.recipe = recipe;
        updateEntries();
    }

    protected abstract void updateEntries();

    @Override
    public final Optional<ResourceLocation> getRecipeLocation() {
        return Optional.of(recipe.getId());
    }

    @Override
    public final ResourceLocation getRecipeCategory() {
        return recipe.getType().registryName;
    }
}
