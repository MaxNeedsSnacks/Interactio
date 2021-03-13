package ky.someone.mods.interactio.integration.jei.categories;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.integration.jei.util.TooltipCallbacks;
import ky.someone.mods.interactio.recipe.BlockAnvilSmashingRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockAnvilSmashingCategory implements IRecipeCategory<BlockAnvilSmashingRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.BLOCK_ANVIL_SMASHING.registryName;

//    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final IDrawable anvil;

    private final String localizedName;

    private final int width = 160;
    private final int height = 120;

    public BlockAnvilSmashingCategory(IGuiHelper guiHelper) {
//        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(width, height);
        overlay = guiHelper.createDrawable(Interactio.id("textures/gui/anvil_smashing.png"), 0, 0, width, height);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.ANVIL));

        anvil = guiHelper.createDrawableIngredient(new ItemStack(Items.ANVIL));

        localizedName = Utils.translate("interactio.jei.block_anvil_smashing", null).getString();
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public Class<BlockAnvilSmashingRecipe> getRecipeClass() {
        return BlockAnvilSmashingRecipe.class;
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
    public void setIngredients(BlockAnvilSmashingRecipe recipe, IIngredients ingredients) {
        // display input block as item
        ingredients.setInputs(VanillaTypes.ITEM, recipe.getBlockInput().getMatching()
                .stream()
                .map(Block::asItem)
                .map(Item::getDefaultInstance)
                .collect(Collectors.toList()));

        // display resulting block as item, as well
        if (recipe.getOutput().isBlock()) {
            ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput()
                    .blockOutput
                    .stream()
                    .map(WeightedOutput.WeightedEntry::getResult)
                    .map(Block::asItem)
                    .map(Item::getDefaultInstance)
                    .collect(Collectors.toList())));
        } else if (recipe.getOutput().isItem()) {
            ingredients.setOutputLists(VanillaTypes.ITEM, Collections.singletonList(recipe.getOutput()
                    .itemOutput
                    .stream()
                    .map(WeightedOutput.WeightedEntry::getResult)
                    .collect(Collectors.toList())));
        }
    }

    private final Point center = new Point(45, 52);

    @Override
    public void setRecipe(IRecipeLayout layout, BlockAnvilSmashingRecipe recipe, IIngredients ingredients) {

        List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);

        IGuiItemStackGroup itemStackGroup = layout.getItemStacks();

        WeightedOutput<ItemStack> output;

        if (recipe.getOutput().isItem()) {
            output = recipe.getOutput().itemOutput;
        } else if (recipe.getOutput().isBlock()) {
            WeightedOutput<Block> blocks = recipe.getOutput().blockOutput;
            output = new WeightedOutput<>(blocks.emptyWeight);
            blocks.forEach(entry -> output.add(entry.getResult().asItem().getDefaultInstance(), entry.getWeight()));
        } else {
            output = new WeightedOutput<>(0.0D);
        }

        WeightedOutput.WeightedEntry<ItemStack> empty = new WeightedOutput.WeightedEntry<>(Items.BARRIER.getDefaultInstance(), output.emptyWeight);

        itemStackGroup.init(0, true, center.x, center.y);
        itemStackGroup.init(1, false, width - 20, center.y);

        if (output.emptyWeight > 0) outputs.get(0).add(empty.getResult());
        itemStackGroup.set(ingredients);

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, recipe.getOutput().isItem(), entry -> entry.getResult().equals(stack, false));
        });

    }

    @Override
    public void draw(BlockAnvilSmashingRecipe recipe, PoseStack ms, double mouseX, double mouseY) {

        RenderSystem.enableBlend();

        overlay.draw(ms);

        RenderSystem.disableBlend();

        anvil.draw(ms, center.x + 1, center.y - 32);

    }

}
