package ky.someone.interactio.fabric.rei.categories;

import ky.someone.interactio.fabric.rei.displays.ItemFluidTransformDisplay;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
import me.shedaniel.rei.api.RecipeCategory;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

public class ItemFluidTransformCategory implements RecipeCategory<ItemFluidTransformDisplay> {

    public static final ResourceLocation UID = InWorldRecipeType.ITEM_FLUID_TRANSFORM.registryName;

    @Override
    public ResourceLocation getIdentifier() {
        return UID;
    }

    @Override
    public String getCategoryName() {
        return I18n.get("interactio.recipe_info.item_fluid_transform");
    }

}
