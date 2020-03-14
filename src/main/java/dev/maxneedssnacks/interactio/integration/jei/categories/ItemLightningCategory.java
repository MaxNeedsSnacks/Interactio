package dev.maxneedssnacks.interactio.integration.jei.categories;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ItemLightningRecipe;
import dev.maxneedssnacks.interactio.recipe.ingredient.RecipeIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemLightningCategory implements IRecipeCategory<ItemLightningRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.ITEM_LIGHTNING.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public ItemLightningCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/lightning.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.NETHER_STAR));

        localizedName = Utils.translate("interactio.jei.item_lightning", null);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<ItemLightningRecipe> getRecipeClass() {
        return ItemLightningRecipe.class;
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
    public void setIngredients(ItemLightningRecipe recipe, IIngredients ingredients) {

        List<RecipeIngredient> inputs = ImmutableList.copyOf(recipe.getInputs());

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
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput().stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .collect(Collectors.toList())));
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, ItemLightningRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        double angleDelta = 360.0 / inputs.size();

        Point point = new Point(center.x, 8);

        List<Double> returnChances = recipe.getInputs().stream().map(RecipeIngredient::getReturnChance).collect(Collectors.toList());

        WeightedOutput<ItemStack> output = recipe.getOutput();
        WeightedOutput.WeightedEntry<ItemStack> empty = new WeightedOutput.WeightedEntry<>(Items.BARRIER.getDefaultInstance(), output.emptyWeight);

        int i = 0;
        for (List<ItemStack> input : inputs) {
            itemStackGroup.init(i, true, point.x, point.y);
            itemStackGroup.set(i, input);
            i++;
            point = Utils.rotatePointAbout(point, center, angleDelta);
        }

        itemStackGroup.init(++i, false, width - 20, center.y);
        if (output.emptyWeight > 0) outputs.get(0).add(empty.getResult());
        itemStackGroup.set(i, outputs.get(0));

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            if (input) {
                if (idx >= 0 && idx < returnChances.size()) {
                    double returnChance = returnChances.get(idx);
                    if (returnChance != 0) {
                        tooltip.add(Utils.translate("interactio.jei.return_chance", null, Utils.formatChance(returnChance, TextFormatting.ITALIC)));
                    }
                }
            } else if (!output.isSingle()) {
                WeightedOutput.WeightedEntry<ItemStack> match = output.stream()
                        .filter(entry -> entry.getResult() == stack)
                        .findFirst()
                        .orElse(empty);

                if (match == empty) {
                    tooltip.clear();
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_empty", new Style().setBold(true)));
                }

                tooltip.add(Utils.translate("interactio.jei.weighted_output_chance", null, Utils.formatChance(output.getChance(match), TextFormatting.ITALIC)));

                if (output.rolls > 1) {
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_roll_count",
                            new Style().setColor(TextFormatting.GRAY),
                            output.unique ? Utils.translate("interactio.jei.weighted_output_roll_unique", null, output.rolls)
                                    : output.rolls));
                }
            }
        });


    }

    @Override
    public void draw(ItemLightningRecipe recipe, double mouseX, double mouseY) {

        RenderSystem.enableAlphaTest();
        RenderSystem.enableBlend();

        overlay.draw();

        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();

        guiHelper.getSlotDrawable().draw(width - 20, center.y);

    }

}
