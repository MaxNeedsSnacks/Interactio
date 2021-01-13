package dev.maxneedssnacks.interactio.jei.util;

import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ingredient.WeightedOutput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.function.Predicate;

public final class TooltipCallbacks {

    public static void returnChance(int idx, boolean input, List<ITextComponent> tooltip, List<Double> returnChances) {
        if (input && (idx >= 0) && (idx < returnChances.size())) {
            double returnChance = returnChances.get(idx);
            if (returnChance != 0) {
                tooltip.add(Utils.translate("interactio.jei.return_chance", null, Utils.formatChance(returnChance, TextFormatting.ITALIC)));
            }
        }
    }

    public static <T> void weightedOutput(boolean input, T stack, List<ITextComponent> tooltip, WeightedOutput<T> output, WeightedOutput.WeightedEntry<T> empty, boolean allowMultiple) {
        weightedOutput(input, stack, tooltip, output, empty, allowMultiple, entry -> entry.getResult() == stack);
    }

    public static <T> void weightedOutput(boolean input, T stack, List<ITextComponent> tooltip, WeightedOutput<T> output, WeightedOutput.WeightedEntry<T> empty, boolean allowMultiple, Predicate<WeightedOutput.WeightedEntry<T>> equals) {
        if (!input) {
            if (!output.isSingle()) {
                WeightedOutput.WeightedEntry<T> match = output.stream()
                        .filter(equals)
                        .findFirst()
                        .orElse(empty);

                if (match == empty) {
                    tooltip.clear();
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_empty", Style.EMPTY.setBold(true)));
                }

                tooltip.add(Utils.translate("interactio.jei.weighted_output_chance", null, Utils.formatChance(output.getChance(match), TextFormatting.ITALIC)));

                if (allowMultiple && output.rolls > 1) {
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_roll_count",
                            Style.EMPTY.applyFormatting(TextFormatting.GRAY),
                            output.unique ? Utils.translate("interactio.jei.weighted_output_roll_unique", null, output.rolls)
                                    : output.rolls));
                }
            }
        }
    }

}
