package dev.maxneedssnacks.interactio.integration.jei.categories;

import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.compat.CompatUtil;
import dev.maxneedssnacks.interactio.integration.jei.IconRecipeInfo;
import dev.maxneedssnacks.interactio.recipe.FluidFluidTransformRecipe;
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
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FluidFluidTransformCategory implements IRecipeCategory<FluidFluidTransformRecipe> {

    public static final ResourceLocation UID = Interactio.id("fluid_fluid_transform");

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;


    private final int width = 160;
    private final int height = 120;

    public FluidFluidTransformCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/fluid_transform.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new FluidStack(Fluids.FLOWING_WATER, 1000));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<FluidFluidTransformRecipe> getRecipeClass() {
        return FluidFluidTransformRecipe.class;
    }

    @Override
    public String getTitle() {
        // FIXME: localisation
        return "Fluid Transformation";
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
    public void setIngredients(FluidFluidTransformRecipe recipe, IIngredients ingredients) {

        Object2IntMap<Ingredient> items = Object2IntMaps.unmodifiable(recipe.getItems());

        List<List<ItemStack>> mappedItems = new ArrayList<>();

        items.forEach(((ingr, count) -> mappedItems.add(
                Arrays.stream(ingr.getMatchingStacks())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(count))
                        .collect(Collectors.toList()))));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedItems);

        // fluid input
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(new ArrayList<>(recipe.getFluid().getMatchingStacks())));

        // fluid output
        ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(recipe.getOutputFluid(), 1000));
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, FluidFluidTransformRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<FluidStack>> fluid = ingredients.getInputs(VanillaTypes.FLUID);
        List<List<FluidStack>> outputs = ingredients.getOutputs(VanillaTypes.FLUID);

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

        fluidStackGroup.init(0, true, center.x + 1, center.y + 1);
        fluidStackGroup.set(0, fluid.get(0));

        fluidStackGroup.init(1, false, width - 24 + 1, center.y + 1);
        fluidStackGroup.set(1, outputs.get(0));

    }

    @Override
    public void draw(FluidFluidTransformRecipe recipe, double mouseX, double mouseY) {

        CompatUtil.drawWithAlpha(overlay);

        guiHelper.getSlotDrawable().draw(center.x, center.y);
        guiHelper.getSlotDrawable().draw(width - 24, center.y);

        if (recipe.consumesItems()) {
            IconRecipeInfo info = new IconRecipeInfo(guiHelper, Collections.singletonList(
                    TextFormatting.UNDERLINE + "Consumes Items"
            ));
            info.draw(width - 48, height - 36);
            info.drawTooltip((int) mouseX, (int) mouseY);
        }

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

}
