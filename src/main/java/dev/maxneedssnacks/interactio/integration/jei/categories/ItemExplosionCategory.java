package dev.maxneedssnacks.interactio.integration.jei.categories;

import com.google.common.collect.ImmutableList;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.compat.CompatUtil;
import dev.maxneedssnacks.interactio.integration.jei.IconRecipeInfo;
import dev.maxneedssnacks.interactio.recipe.ItemExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import dev.maxneedssnacks.interactio.recipe.util.IngredientStack;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemExplosionCategory implements IRecipeCategory<ItemExplosionRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.ITEM_EXPLODE.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final int width = 160;
    private final int height = 120;

    public ItemExplosionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/explode.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.GUNPOWDER));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<ItemExplosionRecipe> getRecipeClass() {
        return ItemExplosionRecipe.class;
    }

    @Override
    public String getTitle() {
        // FIXME: localisation
        return "Exploding Items";
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
    public void setIngredients(ItemExplosionRecipe recipe, IIngredients ingredients) {

        List<IngredientStack> inputs = ImmutableList.copyOf(recipe.getInputs());

        List<List<ItemStack>> mappedInputs = new ArrayList<>();

        inputs.forEach(input -> mappedInputs.add(
                Arrays.stream(input.getIngredient()
                        .getMatchingStacks())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(input.getCount()))
                        .collect(Collectors.toList())));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedInputs);

        // item output
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, ItemExplosionRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        double angleDelta = 360.0 / inputs.size();

        Point point = new Point(center.x, 8);

        int i = 0;
        for (List<ItemStack> input : inputs) {
            itemStackGroup.init(i, true, point.x, point.y);
            itemStackGroup.set(i, input);
            i++;
            point = Utils.rotatePointAbout(point, center, angleDelta);
        }

        itemStackGroup.init(++i, false, width - 20, center.y);
        itemStackGroup.set(i, outputs.get(0));

    }

    @Override
    public void draw(ItemExplosionRecipe recipe, double mouseX, double mouseY) {

        CompatUtil.drawWithAlpha(overlay);

        guiHelper.createDrawableIngredient(new ItemStack(Items.TNT)).draw(center.x, center.y);
        guiHelper.getSlotDrawable().draw(width - 20, center.y);

        if (recipe.getChance() < 1) {
            IconRecipeInfo info = new IconRecipeInfo(guiHelper,
                    TextFormatting.UNDERLINE + "Recipe might fail",
                    "Chance of Success: " + TextFormatting.ITALIC + String.format("%.2f%%", recipe.getChance() * 100.0)
            );
            info.draw(width - 48, height - 36);
            info.drawTooltip((int) mouseX, (int) mouseY);
        }

    }

}
