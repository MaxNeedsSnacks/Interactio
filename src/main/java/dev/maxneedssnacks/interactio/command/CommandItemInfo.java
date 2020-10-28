package dev.maxneedssnacks.interactio.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import dev.maxneedssnacks.interactio.Interactio;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
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

        player.sendMessage(new StringTextComponent("-- Item Info for ").mergeStyle(TextFormatting.GREEN)
                        .append(stack.getDisplayName())
                        .appendString(" --").mergeStyle(TextFormatting.GREEN)
                , Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- ID: ").mergeStyle(TextFormatting.YELLOW)
                .appendString(Objects.toString(stack.getItem().getRegistryName())), Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- Language Key: ").mergeStyle(TextFormatting.YELLOW)
                .appendString(stack.getTranslationKey()), Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- NBT Tag: ").mergeStyle(TextFormatting.YELLOW)
                .appendString(Objects.toString(stack.getTag(), "{}")), Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- List of (Item) Tags:").mergeStyle(TextFormatting.YELLOW), Interactio.CHAT_ID);
        stack.getItem().getTags()
                .stream()
                .sorted()
                .forEachOrdered(id -> {
                    ITextComponent component = new StringTextComponent("\u2022 " + id);
                    component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                    player.sendMessage(component, Interactio.CHAT_ID);
                });

        Item item = stack.getItem();
        if (item instanceof BlockItem) {
            BlockItem bi = (BlockItem) item;
            player.sendMessage(new StringTextComponent("- List of Block Tags:").mergeStyle(TextFormatting.YELLOW), Interactio.CHAT_ID);
            bi.getBlock().getTags()
                    .stream()
                    .sorted()
                    .forEachOrdered(id -> {
                        ITextComponent component = new StringTextComponent("\u2022 " + id);
                        component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                        player.sendMessage(component, Interactio.CHAT_ID);
                    });
            if (!Objects.equals(bi.getBlock().getRegistryName(), item.getRegistryName())) {
                player.sendMessage(new StringTextComponent("- Block ID (when placed in World): ").mergeStyle(TextFormatting.YELLOW)
                        .appendString(Objects.toString(bi.getBlock().getRegistryName())), Interactio.CHAT_ID);
            }
        }

        player.sendMessage(new StringTextComponent("-----------------------------").mergeStyle(TextFormatting.GREEN), Interactio.CHAT_ID);

        return Command.SINGLE_SUCCESS;
    }
}
