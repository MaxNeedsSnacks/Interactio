package dev.maxneedssnacks.interactio.jei.categories;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.jei.util.TooltipCallbacks;
import dev.maxneedssnacks.interactio.recipe.BlockExplosionRecipe;
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
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BlockExplosionCategory implements IRecipeCategory<BlockExplosionRecipe> {

    public static final ResourceLocation UID = InWorldRecipeType.BLOCK_EXPLODE.registryName;

    private final IGuiHelper guiHelper;

    private final IDrawableStatic background;
    //FIXME: Add an overlay. I know. I'm lazy.
    //private final IDrawableStatic overlay;

    private final IDrawable icon;

    private final IDrawable tnt;

    private final String localizedName;

    public BlockExplosionCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;

        background = guiHelper.createBlankDrawable(96, 34);

        icon = guiHelper.createDrawableIngredient(new ItemStack(Items.TNT));

        tnt = guiHelper.createDrawableIngredient(new ItemStack(Items.TNT));

        localizedName = Utils.translate("interactio.jei.block_explode", null).getString();
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
        ingredients.setInputs(VanillaTypes.ITEM, recipe.getInput().getMatchingBlocks()
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

    @Override
    public void setRecipe(IRecipeLayout layout, BlockExplosionRecipe recipe, IIngredients ingredients) {

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

        itemStackGroup.init(0, true, 4, 8);
        itemStackGroup.init(1, false, 72, 8);

        if (output.emptyWeight > 0) outputs.get(0).add(empty.getResult());
        itemStackGroup.set(ingredients);

        itemStackGroup.addTooltipCallback((idx, input, stack, tooltip) -> {
            TooltipCallbacks.weightedOutput(input, stack, tooltip, output, empty, recipe.getOutput().isItem(), entry -> entry.getResult().equals(stack, false));
        });

    }

    @Override
    public void draw(BlockExplosionRecipe recipe, MatrixStack ms, double mouseX, double mouseY) {

        tnt.draw(ms, 38, 9);

        guiHelper.getSlotDrawable().draw(ms, 4, 8);
        guiHelper.getSlotDrawable().draw(ms, 72, 8);

    }

}
