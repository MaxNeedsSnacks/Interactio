package ky.someone.mods.interactio.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.Collections;
import java.util.Objects;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandRegistryDump {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // only register this if no other dump_registry command exists
        if (dispatcher.findNode(Collections.singletonList("dump_registry")) != null) return;

        dispatcher.register(literal("dump_registry")
                .then(argument("registry", RegistryArgument.registry())
                        .executes(ctx -> dumpRegistry(RegistryArgument.getRegistry(ctx, "registry"), ctx.getSource())))
                .executes(ctx -> {
                    CommandSourceStack s = ctx.getSource();
                    s.sendSuccess(new TextComponent("-- Available Registries --").withStyle(ChatFormatting.YELLOW), false);
                    RegistryManager.getRegistryNamesForSyncToClient().forEach(loc -> {
                        s.sendSuccess(
                                new TextComponent("- ")
                                        .append(Objects.toString(loc))
                                        .setStyle(Style.EMPTY
                                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                                        new TextComponent("Click to view dump of ")
                                                                .append(new TextComponent(Objects.toString(loc)).withStyle(ChatFormatting.AQUA))))
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dump_registry " + loc)))
                                , false);
                    });
                    return Command.SINGLE_SUCCESS;
                })
        );
    }

    private static int dumpRegistry(ForgeRegistry<?> reg, CommandSourceStack source) {
        source.sendSuccess(
                new TextComponent("-- Registry Dump for ")
                        .append(reg.getRegistryName().toString())
                        .append(" --")
                        .withStyle(ChatFormatting.YELLOW)
                , false);

        reg.getEntries().forEach(entry -> {
            source.sendSuccess(
                    new TextComponent("- ")
                            .append(Objects.toString(entry.getKey()))
                            .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent(Objects.toString(entry.getValue())))))
                    , false);
        });

        return Command.SINGLE_SUCCESS;
    }

}
