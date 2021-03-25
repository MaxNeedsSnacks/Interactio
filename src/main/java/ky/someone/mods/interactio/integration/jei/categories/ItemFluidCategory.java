package ky.someone.mods.interactio.integration.jei.categories;

import static ky.someone.mods.interactio.Utils.rotatePointAbout;
import static ky.someone.mods.interactio.integration.jei.InteractioJEIPlugin.setOutputLists;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.integration.jei.util.TooltipCallbacks;
import ky.someone.mods.interactio.recipe.ItemFluidRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiFluidStackGroup;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public class ItemFluidCategory implements IRecipeCategory<ItemFluidRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.FLUID_TRANSFORM.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public ItemFluidCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/fluid_transform.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new FluidStack(Fluids.FLOWING_WATER, 1000));

        localizedName = Utils.translate("interactio.jei.fluid_fluid_transform", null).getString();
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<ItemFluidRecipe> getRecipeClass() {
        return ItemFluidRecipe.class;
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
    public void setIngredients(ItemFluidRecipe recipe, IIngredients ingredients) {

        List<ItemIngredient> items = ImmutableList.copyOf(recipe.getItemInputs());

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
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(new ArrayList<>(recipe.getFluidInput().getMatchingStacks())));

        // fluid output
        setOutputLists(ingredients, recipe.getOutput());
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, ItemFluidRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> itemInputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<FluidStack>> fluid = ingredients.getInputs(VanillaTypes.FLUID);
        List<List<ItemStack>> itemOutputs = ingredients.getOutputs(VanillaTypes.ITEM);
        List<List<FluidStack>> fluidOutputs = ingredients.getOutputs(VanillaTypes.FLUID);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();
        IGuiFluidStackGroup fluidStackGroup = layout.getFluidStacks();

        double angleDelta = 360.0 / itemInputs.size();

        Point point = new Point(center.x, 8);

        List<Double> returnChances = recipe.getItemInputs().stream().map(ItemIngredient::getReturnChance).collect(Collectors.toList());

        int i = 0;
        for (List<ItemStack> input : itemInputs) {
            itemStackGroup.init(i, true, point.x, point.y);
            itemStackGroup.set(i, input);
            i++;
            point = rotatePointAbout(point, center, angleDelta);
        }

        fluidStackGroup.init(0, true, center.x + 1, center.y + 1);
        fluidStackGroup.set(0, fluid.get(0));

        if (recipe.getOutput().isFluid()) {
            WeightedOutput<Fluid> output = recipe.getOutput().fluidOutput;
            WeightedOutput.WeightedEntry<Fluid> empty = new WeightedOutput.WeightedEntry<>(Fluids.EMPTY, output.emptyWeight);
        
            fluidStackGroup.init(1, false, width - 20 + 1, center.y + 1);
            if (output.emptyWeight > 0) fluidOutputs.get(0).add(new FluidStack(empty.getResult(), 1000));
            fluidStackGroup.set(1, fluidOutputs.get(0));
            
            itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
                TooltipCallbacks.returnChance(idx, input, tooltip, returnChances);
            });

            fluidStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
                TooltipCallbacks.weightedOutput(input, stack.getFluid(), tooltip, output, empty, false);
            });
        }
        else if (recipe.getOutput().isItem()) {
            WeightedOutput<ItemStack> output = recipe.getOutput().itemOutput;
            WeightedOutput.WeightedEntry<ItemStack> empty = new WeightedOutput.WeightedEntry<>(Items.BARRIER.getDefaultInstance(), output.emptyWeight);
            
            itemStackGroup.init(++i, false, width - 20, center.y);
            if (output.emptyWeight > 0) itemOutputs.get(0).add(empty.getResult());
            itemStackGroup.set(i, itemOutputs.get(0));

            itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
                TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, true);
            });
            
            fluidStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
                if (input && recipe.getConsumeFluid() > 0) {
                    tooltip.add(Utils.translate("interactio.jei.consume_chance", null, Utils.formatChance(recipe.getConsumeFluid(), ChatFormatting.ITALIC)));
                }
            });
        }
    }

    @Override
    public void draw(ItemFluidRecipe recipe, PoseStack ms, double mouseX, double mouseY) {

        RenderSystem.enableBlend();

        overlay.draw(ms);

        RenderSystem.disableBlend();

        guiHelper.getSlotDrawable().draw(ms, center.x, center.y);
        guiHelper.getSlotDrawable().draw(ms, width - 20, center.y);

    }

}
