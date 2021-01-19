package ky.someone.interactio.forge.jei.categories;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ky.someone.interactio.Interactio;
import ky.someone.interactio.Utils;
import ky.someone.interactio.forge.jei.util.ForgeFluidUtils;
import ky.someone.interactio.forge.jei.util.TooltipCallbacks;
import ky.someone.interactio.recipe.ItemFluidTransformRecipe;
import ky.someone.interactio.recipe.ingredient.RecipeIngredient;
import ky.someone.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
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
import net.minecraftforge.fluids.FluidStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemFluidTransformCategory implements IRecipeCategory<ItemFluidTransformRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.ITEM_FLUID_TRANSFORM.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public ItemFluidTransformCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/fluid_transform.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.BUCKET));

        localizedName = Utils.translate("interactio.recipe_info.item_fluid_transform", null).getString();
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
    public void setIngredients(ItemFluidTransformRecipe recipe, IIngredients ingredients) {

        List<RecipeIngredient> inputs = ImmutableList.copyOf(recipe.getInputs());

        List<List<ItemStack>> mappedInputs = new ArrayList<>();

        inputs.forEach(input -> mappedInputs.add(
                Arrays.stream(input.getIngredient()
                        .getItems())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(input.getCount()))
                        .collect(Collectors.toList())));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedInputs);

        // fluid input
        ingredients.setInputLists(VanillaTypes.FLUID, Collections.singletonList(ForgeFluidUtils.forgeStacksOf(recipe.getFluid().getMatchingStacks())));

        // item output
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput().stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .collect(Collectors.toList())));
    }

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

        fluidStackGroup.init(0, true, center.x + 1, center.y + 1);
        fluidStackGroup.set(0, fluid.get(0));

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.returnChance(idx, input, tooltip, returnChances);
            TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, true);
        });

        fluidStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            if (input && recipe.getConsumeFluid() > 0) {
                tooltip.add(Utils.translate("interactio.recipe_info.consume_chance", null, Utils.formatChance(recipe.getConsumeFluid(), ChatFormatting.ITALIC)));
            }
        });

    }

    @Override
    public void draw(ItemFluidTransformRecipe recipe, PoseStack ms, double mouseX, double mouseY) {

        RenderSystem.enableBlend();

        overlay.draw(ms);

        RenderSystem.disableBlend();

        guiHelper.getSlotDrawable().draw(ms, center.x, center.y);
        guiHelper.getSlotDrawable().draw(ms, width - 20, center.y);

    }

}
