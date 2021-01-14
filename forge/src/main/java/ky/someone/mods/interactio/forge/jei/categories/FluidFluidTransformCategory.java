package ky.someone.mods.interactio.forge.jei.categories;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.forge.jei.util.ForgeFluidUtils;
import ky.someone.mods.interactio.forge.jei.util.TooltipCallbacks;
import ky.someone.mods.interactio.recipe.FluidFluidTransformRecipe;
import ky.someone.mods.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FluidFluidTransformCategory implements IRecipeCategory<FluidFluidTransformRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.FLUID_FLUID_TRANSFORM.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public FluidFluidTransformCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/fluid_transform.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new FluidStack(Fluids.FLOWING_WATER, 1000));

        localizedName = Utils.translate("interactio.recipe_info.fluid_fluid_transform", null).getString();
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
    public void setIngredients(FluidFluidTransformRecipe recipe, IIngredients ingredients) {

        List<RecipeIngredient> items = ImmutableList.copyOf(recipe.getItems());

        List<List<ItemStack>> mappedItems = new ArrayList<>();

        items.forEach(item -> mappedItems.add(
                Arrays.stream(item.getIngredient()
                        .getItems())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(i.getCount() * item.getCount()))
                        .collect(Collectors.toList())));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedItems);

        // fluid input
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(ForgeFluidUtils.forgeStacksOf(recipe.getFluid().getMatchingStacks())));

        // fluid output
        ingredients.setOutputLists(VanillaTypes.FLUID, Collections.singletonList(recipe.getOutput().stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .map(fluid -> new FluidStack(fluid, 1000))
                .collect(Collectors.toList())));

        //ingredients.setOutput(VanillaTypes.FLUID, new FluidStack(recipe.getResult(), 1000));
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

        List<Double> returnChances = recipe.getItems().stream().map(RecipeIngredient::getReturnChance).collect(Collectors.toList());

        WeightedOutput<Fluid> output = recipe.getOutput();
        WeightedOutput.WeightedEntry<Fluid> empty = new WeightedOutput.WeightedEntry<>(Fluids.EMPTY, output.emptyWeight);

        int i = 0;
        for (List<ItemStack> input : inputs) {
            itemStackGroup.init(i, true, point.x, point.y);
            itemStackGroup.set(i, input);
            i++;
            point = Utils.rotatePointAbout(point, center, angleDelta);
        }

        fluidStackGroup.init(0, true, center.x + 1, center.y + 1);
        fluidStackGroup.set(0, fluid.get(0));

        fluidStackGroup.init(1, false, width - 20 + 1, center.y + 1);
        if (output.emptyWeight > 0) outputs.get(0).add(new FluidStack(empty.getResult(), 1000));
        fluidStackGroup.set(1, outputs.get(0));

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.returnChance(idx, input, tooltip, returnChances);
        });

        fluidStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.weightedOutput(input, stack.getFluid(), tooltip, output, empty, false);
        });

    }

    @Override
    public void draw(FluidFluidTransformRecipe recipe, PoseStack ms, double mouseX, double mouseY) {

        RenderSystem.enableBlend();

        overlay.draw(ms);

        RenderSystem.disableBlend();

        guiHelper.getSlotDrawable().draw(ms, center.x, center.y);
        guiHelper.getSlotDrawable().draw(ms, width - 20, center.y);

    }

}
