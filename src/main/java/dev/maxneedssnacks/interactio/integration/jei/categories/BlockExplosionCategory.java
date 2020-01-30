package dev.maxneedssnacks.interactio.integration.jei.categories;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.integration.jei.IconRecipeInfo;
import dev.maxneedssnacks.interactio.recipe.BlockExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class BlockExplosionCategory implements IRecipeCategory<BlockExplosionRecipe> {

    public static final ResourceLocation UID = ModRecipes.BLOCK_EXPLODE;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    //FIXME: Add an overlay. I know. I'm lazy.
    //private final IDrawableStatic overlay;

    private final IDrawable icon;

    public BlockExplosionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(82, 34);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.TNT));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<BlockExplosionRecipe> getRecipeClass() {
        return BlockExplosionRecipe.class;
    }

    @Override
    public String getTitle() {
        // FIXME: localisation
        return "Exploding Blocks";
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setIngredients(BlockExplosionRecipe recipe, IIngredients ingredients) {
        // display input block as item
        ingredients.setInput(VanillaTypes.ITEM, recipe.getInput().asItem().getDefaultInstance());

        // display resulting block as item, as well
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    @Override
    public void setRecipe(IRecipeLayout layout, BlockExplosionRecipe recipe, IIngredients ingredients) {

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        itemStackGroup.init(0, true, 4, 8);
        itemStackGroup.init(1, false, 60, 8);

        itemStackGroup.set(ingredients);

    }

    @Override
    public void draw(BlockExplosionRecipe recipe, double mouseX, double mouseY) {

        List<String> tooltips = Lists.newArrayList(
                TextFormatting.UNDERLINE + "Explosion Recipe",
                "Chance of Success: " + TextFormatting.ITALIC + String.format("%.2f%%", recipe.getChance() * 100.0)
        );

        if (recipe.isDestroy()) {
            tooltips.add(TextFormatting.ITALIC + "Destroys Blocks");
        }

        IconRecipeInfo info = new IconRecipeInfo(new ItemStack(Items.TNT), guiHelper, tooltips);
        info.draw(32, 9);
        info.drawTooltip((int) mouseX, (int) mouseY);

    }

}
