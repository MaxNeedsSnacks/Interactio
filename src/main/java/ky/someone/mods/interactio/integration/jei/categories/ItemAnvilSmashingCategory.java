package ky.someone.mods.interactio.integration.jei.categories;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.integration.jei.util.TooltipCallbacks;
import ky.someone.mods.interactio.recipe.ItemAnvilSmashingRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemAnvilSmashingCategory implements IRecipeCategory<ItemAnvilSmashingRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.ITEM_ANVIL_SMASHING.registryName;

//    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final IDrawable anvil;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public ItemAnvilSmashingCategory(IGuiHelper guiHelper) {
//        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/anvil_smashing.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.ANVIL));

        anvil = guiHelper.createDrawableIngredient(new ItemStack(Items.ANVIL));

        localizedName = Utils.translate("interactio.jei.item_anvil_smashing", null).getString();
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<? extends ItemAnvilSmashingRecipe> getRecipeClass() {
        return ItemAnvilSmashingRecipe.class;
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
    public void setIngredients(ItemAnvilSmashingRecipe recipe, IIngredients ingredients) {

        List<ItemIngredient> inputs = ImmutableList.copyOf(recipe.getItemInputs());

        List<List<ItemStack>> mappedInputs = new ArrayList<>();

        inputs.forEach(input -> mappedInputs.add(
                Arrays.stream(input.getIngredient()
                        .getItems())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(input.getCount()))
                        .collect(Collectors.toList())));

        // item inputs
        ingredients.setInputLists(VanillaTypes.ITEM, mappedInputs);

        // item output
        ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput().itemOutput.stream()
                .map(WeightedOutput.WeightedEntry::getResult)
                .collect(Collectors.toList())));
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, ItemAnvilSmashingRecipe recipe, IIngredients ingredients) {

        // TODO: replace radial drawing with simple list draw

        List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        double angleDelta = 360.0 / inputs.size();

        Point point = new Point(center.x, 8);

        List<Double> returnChances = recipe.getItemInputs().stream().map(ItemIngredient::getReturnChance).collect(Collectors.toList());

        WeightedOutput<ItemStack> output = recipe.getOutput().itemOutput;
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
            TooltipCallbacks.returnChance(idx, input, tooltip, returnChances);
            TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, true);
        });

    }

    @Override
    public void draw(ItemAnvilSmashingRecipe recipe, PoseStack ms, double mouseX, double mouseY) {

        RenderSystem.enableBlend();

        overlay.draw(ms);

        RenderSystem.disableBlend();

        anvil.draw(ms, center.x, center.y);

    }

}
