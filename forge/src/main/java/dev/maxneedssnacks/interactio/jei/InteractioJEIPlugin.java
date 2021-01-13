package dev.maxneedssnacks.interactio.jei;

import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.jei.categories.*;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.util.ResourceLocation;

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
                new ItemExplosionCategory(guiHelper),
                new BlockExplosionCategory(guiHelper),
                new ItemLightningCategory(guiHelper),
                new ItemAnvilSmashingCategory(guiHelper),
                new BlockAnvilSmashingCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InWorldRecipeType.ITEM_FLUID_TRANSFORM.getRecipes(), ItemFluidTransformCategory.UID);
        registration.addRecipes(InWorldRecipeType.FLUID_FLUID_TRANSFORM.getRecipes(), FluidFluidTransformCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_EXPLODE.getRecipes(), ItemExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_EXPLODE.getRecipes(), BlockExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_LIGHTNING.getRecipes(), ItemLightningCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_ANVIL_SMASHING.getRecipes(), ItemAnvilSmashingCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_ANVIL_SMASHING.getRecipes(), BlockAnvilSmashingCategory.UID);
    }
}
