package dev.maxneedssnacks.interactio.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;

import java.util.Collections;
import java.util.Objects;

public class CommandItemInfo {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        // only register this if no other item_info command exists
        if (dispatcher.findNode(Collections.singletonList("item_info")) != null) return;

        dispatcher.register(Commands.literal("item_info")
                .executes(context -> printItem(context.getSource().asPlayer()))
        );
    }

    private static int printItem(ServerPlayerEntity player) {
        ItemStack stack = player.getHeldItem(Hand.MAIN_HAND);

        player.sendMessage(new StringTextComponent("-- Item Info for ").applyTextStyle(TextFormatting.GREEN)
                .appendSibling(stack.getDisplayName())
                .appendText(" --").applyTextStyle(TextFormatting.GREEN)
        );

        player.sendMessage(new StringTextComponent("- ID: ").applyTextStyle(TextFormatting.YELLOW)
                .appendText(Objects.toString(stack.getItem().getRegistryName())));

        player.sendMessage(new StringTextComponent("- Language Key: ").applyTextStyle(TextFormatting.YELLOW)
                .appendText(stack.getTranslationKey()));

        player.sendMessage(new StringTextComponent("- NBT Tag: ").applyTextStyle(TextFormatting.YELLOW)
                .appendText(Objects.toString(stack.getTag(), "{}")));

        player.sendMessage(new StringTextComponent("- List of (Item) Tags:").applyTextStyle(TextFormatting.YELLOW));
        stack.getItem().getTags()
                .stream()
                .sorted()
                .forEachOrdered(id -> {
                    ITextComponent component = new StringTextComponent("\u2022 " + id);
                    component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                    player.sendMessage(component);
                });

        player.sendMessage(new StringTextComponent("-----------------------------").applyTextStyle(TextFormatting.GREEN));

        return Command.SINGLE_SUCCESS;
    }
}
