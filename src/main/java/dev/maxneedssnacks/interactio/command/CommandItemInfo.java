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

        // func_240699_a_ = applyTextStyle
        // func_230529_a_ = appendSibling
        // func_240702_b_ = appendText
        player.sendMessage(new StringTextComponent("-- Item Info for ").func_240699_a_(TextFormatting.GREEN)
                        .func_230529_a_(stack.getDisplayName())
                        .func_240702_b_(" --").func_240699_a_(TextFormatting.GREEN)
                , Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- ID: ").func_240699_a_(TextFormatting.YELLOW)
                .func_240702_b_(Objects.toString(stack.getItem().getRegistryName())), Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- Language Key: ").func_240699_a_(TextFormatting.YELLOW)
                .func_240702_b_(stack.getTranslationKey()), Interactio.CHAT_ID);

        player.sendMessage(new StringTextComponent("- NBT Tag: ").func_240699_a_(TextFormatting.YELLOW)
                .func_240702_b_(Objects.toString(stack.getTag(), "{}")), Interactio.CHAT_ID);

        // func_240715_a_ = setClickEvent
        player.sendMessage(new StringTextComponent("- List of (Item) Tags:").func_240699_a_(TextFormatting.YELLOW), Interactio.CHAT_ID);
        stack.getItem().getTags()
                .stream()
                .sorted()
                .forEachOrdered(id -> {
                    ITextComponent component = new StringTextComponent("\u2022 " + id);
                    component.getStyle().func_240715_a_(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                    player.sendMessage(component, Interactio.CHAT_ID);
                });

        Item item = stack.getItem();
        if (item instanceof BlockItem && !Objects.equals(((BlockItem) item).getBlock().getRegistryName(), item.getRegistryName())) {
            player.sendMessage(new StringTextComponent("- Block ID (when placed in World): ").func_240699_a_(TextFormatting.YELLOW)
                    .func_240702_b_(Objects.toString(((BlockItem) item).getBlock().getRegistryName())), Interactio.CHAT_ID);
        }

        player.sendMessage(new StringTextComponent("-----------------------------").func_240699_a_(TextFormatting.GREEN), Interactio.CHAT_ID);

        return Command.SINGLE_SUCCESS;
    }
}
