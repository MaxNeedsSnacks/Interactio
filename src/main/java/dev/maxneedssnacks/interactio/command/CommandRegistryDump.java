package dev.maxneedssnacks.interactio.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class CommandRegistryDump {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        // only register this if no other dump_registry command exists
        if (dispatcher.findNode(Collections.singletonList("dump_registry")) != null) return;

        // func_240699_a_ = applyTextStyle
        // func_230529_a_ = appendSibling
        // func_240702_b_ = appendText
        // func_230530_a_ = setStyle
        // field_240709_b_ = empty
        // func_240715_a_ = setClickEvent
        // func_240716_a_ = setHoverEvent
        // field_230550_a_ = SHOW_TEXT
        dispatcher.register(literal("dump_registry")
                .then(argument("registry", RegistryArgument.registry())
                        .executes(ctx -> dumpRegistry(RegistryArgument.getRegistry(ctx, "registry"), ctx.getSource())))
                .executes(ctx -> {
                    CommandSource s = ctx.getSource();
                    s.sendFeedback(new StringTextComponent("-- Available Registries --").func_240699_a_(TextFormatting.YELLOW), false);
                    RegistryManager.getRegistryNamesForSyncToClient().forEach(loc -> {
                        s.sendFeedback(
                                new StringTextComponent("- ")
                                        .func_240702_b_(Objects.toString(loc))
                                        .func_230530_a_(Style.field_240709_b_
                                                .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_,
                                                        new StringTextComponent("Click to view dump of ")
                                                                .func_230529_a_(new StringTextComponent(Objects.toString(loc)).func_240699_a_(TextFormatting.AQUA))))
                                                .func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dump_registry " + loc)))
                                , false);
                    });
                    return Command.SINGLE_SUCCESS;
                })
        );
    }

    private static int dumpRegistry(ForgeRegistry<?> reg, CommandSource source) {
        source.sendFeedback(
                new StringTextComponent("-- Registry Dump for ")
                        .func_240702_b_(reg.getRegistryName().toString())
                        .func_240702_b_(" --")
                        .func_240699_a_(TextFormatting.YELLOW)
                , false);

        reg.getEntries().forEach(entry -> {
            source.sendFeedback(
                    new StringTextComponent("- ")
                            .func_240702_b_(Objects.toString(entry.getKey()))
                            .func_230530_a_(Style.field_240709_b_.func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent(Objects.toString(entry.getValue())))))
                    , false);
        });

        return Command.SINGLE_SUCCESS;
    }

    public static class RegistryArgument implements ArgumentType<ForgeRegistry<?>> {

        private final List<ResourceLocation> registries = RegistryManager.getRegistryNamesForSyncToClient();

        private static final SimpleCommandExceptionType BAD_REGISTRY = new SimpleCommandExceptionType(new StringTextComponent("Registry does not exist"));

        public static ForgeRegistry<?> getRegistry(CommandContext<CommandSource> ctx, String name) {
            return ctx.getArgument(name, ForgeRegistry.class);
        }

        public static RegistryArgument registry() {
            return new RegistryArgument();
        }

        @Override
        public ForgeRegistry<?> parse(StringReader reader) throws CommandSyntaxException {
            int i = reader.getCursor();
            ResourceLocation resLoc = ResourceLocation.read(reader);

            ForgeRegistry<?> reg = RegistryManager.ACTIVE.getRegistry(resLoc);

            if (reg == null) {
                throw BAD_REGISTRY.createWithContext(reader);
            }

            return reg;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return ISuggestionProvider.suggestIterable(registries, builder);
        }

    }

}
