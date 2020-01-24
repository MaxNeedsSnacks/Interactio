package dev.maxneedssnacks.interactio.integration.jei;

import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.integration.jei.categories.FluidFluidTransformCategory;
import dev.maxneedssnacks.interactio.integration.jei.categories.ItemExplosionCategory;
import dev.maxneedssnacks.interactio.integration.jei.categories.ItemFluidTransformCategory;
import dev.maxneedssnacks.interactio.recipe.FluidFluidTransformRecipe;
import dev.maxneedssnacks.interactio.recipe.ItemExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ItemFluidTransformRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class InteractioJEIPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = Interactio.id("jei");
    public static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime r) {
        runtime = r;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new ItemFluidTransformCategory(guiHelper),
                new FluidFluidTransformCategory(guiHelper),
                new ItemExplosionCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(getRecipes(ItemFluidTransformRecipe.class), ItemFluidTransformCategory.UID);
        registration.addRecipes(getRecipes(FluidFluidTransformRecipe.class), FluidFluidTransformCategory.UID);
        registration.addRecipes(getRecipes(ItemExplosionRecipe.class), ItemExplosionCategory.UID);
    }

    private static <T extends InWorldRecipe<?, ?, ?>> List<T> getRecipes(Class<T> clz) {
        return Utils.getInWorldRecipeList(clz).orElse(Collections.emptyList());
    }
}
