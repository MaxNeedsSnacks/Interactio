package ky.someone.interactio.fabric.rei.displays;

import com.google.common.collect.ImmutableList;
import ky.someone.interactio.recipe.ItemFluidTransformRecipe;
import me.shedaniel.rei.api.EntryStack;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ky.someone.interactio.fabric.rei.util.MathUtils.reiFraction;

public class ItemFluidTransformDisplay extends InWorldRecipeDisplay<ItemFluidTransformRecipe> {

    private List<List<EntryStack>> inputs;
    private List<EntryStack> itemOutputs;

    public ItemFluidTransformDisplay(ItemFluidTransformRecipe recipe) {
        super(recipe);
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void updateEntries() {
        ImmutableList.Builder<List<EntryStack>> inputListBuilder = ImmutableList.builder();

        recipe.getInputs().forEach(input -> inputListBuilder.add(
                Arrays.stream(input.getIngredient()
                        .getItems())
                        .map(ItemStack::copy)
                        .peek(i -> i.setCount(input.getCount()))
                        .map(EntryStack::create)
                        .collect(ImmutableList.toImmutableList()))
        );

        // FIXME: see if there's a good way to differentiate item / fluid entry stack lists in REI
        inputListBuilder.add(recipe.getFluid()
                .getMatchingStacks()
                .stream()
                .map(fs -> EntryStack.create(fs.getFluid(), reiFraction(fs.getAmount())))
                .collect(Collectors.toList())
        );

        inputs = inputListBuilder.build();
    }

    @Override
    public List<List<EntryStack>> getInputEntries() {
        return inputs;
    }
}
