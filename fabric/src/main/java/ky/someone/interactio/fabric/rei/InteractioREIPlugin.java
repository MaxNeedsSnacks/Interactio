package ky.someone.interactio.fabric.rei;

import ky.someone.interactio.Interactio;
import me.shedaniel.rei.api.RecipeHelper;
import me.shedaniel.rei.api.plugins.REIPluginV0;
import net.minecraft.resources.ResourceLocation;

public class InteractioREIPlugin implements REIPluginV0 {

    private static final ResourceLocation PLUGIN_UID = Interactio.id("rei");

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public ResourceLocation getPluginIdentifier() {
        return PLUGIN_UID;
    }

    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {
    }

    @Override
    public void registerRecipeDisplays(RecipeHelper recipeHelper) {
    }
}
