package dev.maxneedssnacks.interactio.integration.jei.categories;

import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.compat.CompatUtil;
import dev.maxneedssnacks.interactio.recipe.ItemFluidTransformRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.config.HoverChecker;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemFluidTransformCategory implements IRecipeCategory<ItemFluidTransformRecipe> {

    public static final ResourceLocation UID = Interactio.id("item_fluid_transform");

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final int width = 160;
    private final int height = 120;

    public ItemFluidTransformCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/fluid_transform.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.BUCKET));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<ItemFluidTransformRecipe> getRecipeClass() {
        return ItemFluidTransformRecipe.class;
    }

    @Override
    public String getTitle() {
        // FIXME: localisation
        return "Fluid to Item Crafting";
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
    public void setIngredients(ItemFluidTransformRecipe recipe, IIngredients ingredients) {

        Object2IntMap<Ingredient> inputs = Object2IntMaps.unmodifiable(recipe.getInputs());

        List<List<ItemStack>> mappedInputs = new ArrayList<>();

        inputs.forEach(((ingr, count) -> mappedInputs.add(
                Arrays.stream(ingr.getMatchingStacks())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(count))
                        .collect(Collectors.toList()))));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedInputs);

        // fluid input
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(new ArrayList<>(recipe.getFluid().getMatchingStacks())));

        // item output
        ingredients.setOutput(VanillaTypes.ITEM, recipe.getRecipeOutput());
    }

    /* This old version uses a "table" view for inputs which is... meh
    @Override
    public void setRecipe(IRecipeLayout recipeLayout, ItemFluidTransformRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<FluidStack>> fluid = ingredients.getInputs(VanillaTypes.FLUID);
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = recipeLayout.getItemStacks();
        IGuiFluidStackGroup fluidStackGroup = recipeLayout.getFluidStacks();

        int i = 0;
        int size = inputs.size();

        int itemsPerRow = 4;
        int rows = ((size - 1) / itemsPerRow + 1);

        int initialY = (height - (rows * 18)) / 2;

        while (i < size) {
            List<ItemStack> input = inputs.get(i);
            itemStackGroup.init(i, true, 18 * (i % itemsPerRow), initialY + (i / itemsPerRow) * 18);
            itemStackGroup.set(i, input);
            i++;
        }

        itemStackGroup.init(++i, false, 153, (height / 2) - 9);
        itemStackGroup.set(i, outputs.get(0));

        fluidStackGroup.init(0, true, 108, (height / 2) - 8);
        fluidStackGroup.set(0, fluid.get(0));
        if (recipe.consumesFluid()) {
            fluidStackGroup.addTooltipCallback(((_0, _1, _2, tooltip) -> {
                if (tooltip.isEmpty()) return;
                String modName = tooltip.remove(tooltip.size() - 1);
                tooltip.add(TextFormatting.RED.toString() + TextFormatting.ITALIC.toString() + "(Will be consumed)");
                tooltip.add(modName);
            }));
        }

    }
    */

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, ItemFluidTransformRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<FluidStack>> fluid = ingredients.getInputs(VanillaTypes.FLUID);
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();
        IGuiFluidStackGroup fluidStackGroup = layout.getFluidStacks();

        double angleDelta = 360.0 / inputs.size();

        Point point = new Point(center.x, 8);

        int i = 0;
        for (List<ItemStack> input : inputs) {
            itemStackGroup.init(i, true, point.x, point.y);
            itemStackGroup.set(i, input);
            i++;
            point = rotatePointAbout(point, center, angleDelta);
        }

        itemStackGroup.init(++i, false, width - 20, center.y);
        itemStackGroup.set(i, outputs.get(0));

        fluidStackGroup.init(0, true, center.x + 1, center.y + 1);
        fluidStackGroup.set(0, fluid.get(0));

    }

    @Override
    public void draw(ItemFluidTransformRecipe recipe, double mouseX, double mouseY) {

        CompatUtil.drawWithAlpha(overlay);

        if (recipe.consumeChance() > 0) {
            IconConsume info = new IconConsume(recipe.consumeChance());
            info.draw(width - 48, height - 36);
            info.drawTooltip((int) mouseX, (int) mouseY);
        }

        guiHelper.getSlotDrawable().draw(center.x, center.y);
        guiHelper.getSlotDrawable().draw(width - 20, center.y);

    }

    /**
     * NOTE: This method originally stems from the Botania mod by Vazkii, which is Open Source
     * and distributed under the Botania License (see http://botaniamod.net/license.php)
     * <p>
     * Find the original Botania GitHub repository here: https://github.com/Vazkii/Botania
     * <p>
     * (Original class: vazkii.botania.client.integration.jei.petalapothecary.PetalApothecaryRecipeCategory, created by <williewillus>)
     */
    private Point rotatePointAbout(Point in, Point about, double degrees) {
        double rad = degrees * Math.PI / 180.0;
        double newX = Math.cos(rad) * (in.x - about.x) - Math.sin(rad) * (in.y - about.y) + about.x;
        double newY = Math.sin(rad) * (in.x - about.x) + Math.cos(rad) * (in.y - about.y) + about.y;
        return new Point((int) Math.round(newX), (int) Math.round(newY));
    }

    private class IconConsume implements IDrawable {
        private final IDrawable icon;
        private final HoverChecker hoverChecker;
        private final double chance;

        public IconConsume(double chance) {
            this.icon = guiHelper.createDrawableIngredient(new ItemStack(Items.BARRIER));
            this.hoverChecker = new HoverChecker(0, 0, 0, 0, 0);
            this.chance = chance;
        }

        public int getWidth() {
            return icon.getWidth();
        }

        public int getHeight() {
            return icon.getHeight();
        }

        @Override
        public void draw(int xOffset, int yOffset) {
            icon.draw(xOffset, yOffset);
            hoverChecker.updateBounds(yOffset, yOffset + icon.getHeight(), xOffset, xOffset + icon.getWidth());
        }

        public void drawTooltip(int mx, int my) {
            if (hoverChecker.checkHover(mx, my)) {
                List<String> tooltip = Arrays.asList(
                        TextFormatting.UNDERLINE + "May consume Fluid",
                        "Consumption Chance: " + TextFormatting.ITALIC + String.format("%.2f%%", chance * 100.0)
                );
                Minecraft mc = Minecraft.getInstance();
                MainWindow window = CompatUtil.getMainWindow();
                if (window == null) return;
                GuiUtils.drawHoveringText(ItemStack.EMPTY, tooltip, mx, my, window.getScaledWidth(), window.getScaledHeight(), -1, mc.fontRenderer);
            }
        }
    }
}
