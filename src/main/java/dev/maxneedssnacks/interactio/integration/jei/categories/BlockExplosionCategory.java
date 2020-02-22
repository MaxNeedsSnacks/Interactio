package dev.maxneedssnacks.interactio.integration.jei.categories;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.integration.jei.IconRecipeInfo;
import dev.maxneedssnacks.interactio.recipe.BlockExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
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
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class BlockExplosionCategory implements IRecipeCategory<BlockExplosionRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.BLOCK_EXPLODE.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    //FIXME: Add an overlay. I know. I'm lazy.
    //private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    public BlockExplosionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(82, 34);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.TNT));

        localizedName = Utils.translate("interactio.jei.block_explode", null);
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
        return localizedName;
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
                Utils.translate("interactio.jei.block_explode.info", new Style().setUnderlined(true)),
                Utils.translate("interactio.jei.block_explode.chance", null, Utils.formatChance(recipe.getChance(), TextFormatting.ITALIC))
        );

        if (recipe.isDestroy()) {
            tooltips.add(Utils.translate("interactio.jei.block_explode.destroy", new Style().setBold(true).setItalic(true)));
        }

        IconRecipeInfo info = new IconRecipeInfo(new ItemStack(Items.TNT), guiHelper, tooltips);
        info.draw(32, 9);
        info.drawTooltip((int) mouseX, (int) mouseY);

    }

}
