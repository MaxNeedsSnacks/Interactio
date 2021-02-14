package ky.someone.mods.interactio.integration.jei.util;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.function.Predicate;

public final class TooltipCallbacks {

    public static void returnChance(int idx, boolean input, List<Component> tooltip, List<Double> returnChances) {
        if (input && (idx >= 0) && (idx < returnChances.size())) {
            double returnChance = returnChances.get(idx);
            if (returnChance != 0) {
                tooltip.add(Utils.translate("interactio.jei.return_chance", null, Utils.formatChance(returnChance, ChatFormatting.ITALIC)));
            }
        }
    }

    public static <T> void weightedOutput(boolean input, T stack, List<Component> tooltip, WeightedOutput<T> output, WeightedOutput.WeightedEntry<T> empty, boolean allowMultiple) {
        weightedOutput(input, stack, tooltip, output, empty, allowMultiple, entry -> entry.getResult() == stack);
    }

    public static <T> void weightedOutput(boolean input, T stack, List<Component> tooltip, WeightedOutput<T> output, WeightedOutput.WeightedEntry<T> empty, boolean allowMultiple, Predicate<WeightedOutput.WeightedEntry<T>> equals) {
        if (!input) {
            if (!output.isSingle()) {
                WeightedOutput.WeightedEntry<T> match = output.stream()
                        .filter(equals)
                        .findFirst()
                        .orElse(empty);

                if (match == empty) {
                    tooltip.clear();
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_empty", Style.EMPTY.withBold(true)));
                }

                tooltip.add(Utils.translate("interactio.jei.weighted_output_chance", null, Utils.formatChance(output.getChance(match), ChatFormatting.ITALIC)));

                if (allowMultiple && output.rolls > 1) {
                    tooltip.add(Utils.translate("interactio.jei.weighted_output_roll_count",
                            Style.EMPTY.applyFormat(ChatFormatting.GRAY),
                            output.unique ? Utils.translate("interactio.jei.weighted_output_roll_unique", null, output.rolls)
                                    : output.rolls));
                }
            }
        }
    }

}
