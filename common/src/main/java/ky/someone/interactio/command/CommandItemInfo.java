package ky.someone.interactio.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import ky.someone.interactio.Interactio;
import ky.someone.interactio.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static net.minecraft.commands.Commands.literal;

public class CommandItemInfo {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // only register this if no other item_info command exists
        if (dispatcher.findNode(Collections.singletonList("item_info")) != null) return;

        dispatcher.register(literal("item_info")
                .executes(context -> printItem(context.getSource().getPlayerOrException()))
        );
    }

    private static int printItem(ServerPlayer player) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        Item item = stack.getItem();

        player.sendMessage(new TextComponent("-- Item Info for ").withStyle(ChatFormatting.GREEN)
                        .append(stack.getDisplayName())
                        .append(" --").withStyle(ChatFormatting.GREEN)
                , Interactio.CHAT_ID);

        player.sendMessage(new TextComponent("- ID: ").withStyle(ChatFormatting.YELLOW)
                .append(Objects.toString(Utils.itemId(item))), Interactio.CHAT_ID);

        player.sendMessage(new TextComponent("- Language Key: ").withStyle(ChatFormatting.YELLOW)
                .append(stack.getDescriptionId()), Interactio.CHAT_ID);

        player.sendMessage(new TextComponent("- NBT Tag: ").withStyle(ChatFormatting.YELLOW)
                .append(Objects.toString(stack.getTag(), "{}")), Interactio.CHAT_ID);

        player.sendMessage(new TextComponent("- List of (Item) Tags:").withStyle(ChatFormatting.YELLOW), Interactio.CHAT_ID);
        ItemTags.getAllTags().getAllTags()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(item))
                .map(Map.Entry::getKey)
                .sorted()
                .forEachOrdered(id -> {
                    Component component = new TextComponent("\u2022 " + id);
                    component.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                    player.sendMessage(component, Interactio.CHAT_ID);
                });

        if (item instanceof BlockItem) {
            BlockItem bi = (BlockItem) item;
            player.sendMessage(new TextComponent("- List of Block Tags:").withStyle(ChatFormatting.YELLOW), Interactio.CHAT_ID);
            Block bl = bi.getBlock();
            BlockTags.getAllTags().getAllTags()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().contains(bl))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .forEachOrdered(id -> {
                        Component component = new TextComponent("\u2022 " + id);
                        component.getStyle().withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, id.toString()));
                        player.sendMessage(component, Interactio.CHAT_ID);
                    });
            if (!Objects.equals(Utils.blockId(bl), Utils.itemId(item))) {
                player.sendMessage(new TextComponent("- Block ID (when placed in World): ").withStyle(ChatFormatting.YELLOW)
                        .append(Objects.toString(Utils.blockId(bl))), Interactio.CHAT_ID);
            }
        }

        player.sendMessage(new TextComponent("-----------------------------").withStyle(ChatFormatting.GREEN), Interactio.CHAT_ID);

        return Command.SINGLE_SUCCESS;
    }
}
