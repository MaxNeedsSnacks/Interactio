package ky.someone.mods.interactio.integration.jei;

import java.util.Collections;
import java.util.stream.Collectors;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.integration.jei.categories.BlockAnvilSmashingCategory;
import ky.someone.mods.interactio.integration.jei.categories.BlockExplosionCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemFluidCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemAnvilSmashingCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemExplosionCategory;
import ky.someone.mods.interactio.integration.jei.categories.ItemLightningCategory;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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
                new ItemFluidCategory(guiHelper),
                new ItemExplosionCategory(guiHelper),
                new BlockExplosionCategory(guiHelper),
                new ItemLightningCategory(guiHelper),
                new ItemAnvilSmashingCategory(guiHelper),
                new BlockAnvilSmashingCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(InWorldRecipeType.FLUID_TRANSFORM.getRecipes(), ItemFluidCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_EXPLODE.getRecipes(), ItemExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_EXPLODE.getRecipes(), BlockExplosionCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_LIGHTNING.getRecipes(), ItemLightningCategory.UID);
        registration.addRecipes(InWorldRecipeType.ITEM_ANVIL_SMASHING.getRecipes(), ItemAnvilSmashingCategory.UID);
        registration.addRecipes(InWorldRecipeType.BLOCK_ANVIL_SMASHING.getRecipes(), BlockAnvilSmashingCategory.UID);
    }
    
    public static void setOutputLists(IIngredients ingredients, DynamicOutput output) {
        if (output.isItem()) {
            ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(output.itemOutput.stream()
                    .map(WeightedOutput.WeightedEntry::getResult)
                    .collect(Collectors.toList())));
        }
        else if (output.isBlock()) {
            ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(output.blockOutput.stream()
                    .map(WeightedOutput.WeightedEntry::getResult)
                    .map(ItemStack::new)
                    .collect(Collectors.toList())));
        }
        else if (output.isFluid()) {
            ingredients.setOutputLists(VanillaTypes.FLUID, Collections.singletonList(output.fluidOutput.stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .map(fluid -> new FluidStack(fluid, 1000))
                .collect(Collectors.toList())));
        }
        else throw new IllegalArgumentException("Output is not a valid type!");
    }
}
