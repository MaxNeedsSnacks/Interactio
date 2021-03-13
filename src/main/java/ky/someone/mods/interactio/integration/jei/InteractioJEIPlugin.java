package ky.someone.mods.interactio.integration.jei;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.integration.jei.categories.BlockAnvilSmashingCategory;
import ky.someone.mods.interactio.integration.jei.categories.BlockExplosionCategory;
import ky.someone.mods.interactio.integration.jei.categories.FluidCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemAnvilSmashingCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemExplosionCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemLightningCategory;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

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
                new FluidCategory(guiHelper),
                new ItemExplosionCategory(guiHelper),
                new BlockExplosionCategory(guiHelper),
                new ItemLightningCategory(guiHelper),
                new ItemAnvilSmashingCategory(guiHelper),
                new BlockAnvilSmashingCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InWorldRecipeType.FLUID_TRANSFORM.getRecipes(), FluidCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_EXPLODE.getRecipes(), ItemExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_EXPLODE.getRecipes(), BlockExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_LIGHTNING.getRecipes(), ItemLightningCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_ANVIL_SMASHING.getRecipes(), ItemAnvilSmashingCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_ANVIL_SMASHING.getRecipes(), BlockAnvilSmashingCategory.UID);
    }
}
