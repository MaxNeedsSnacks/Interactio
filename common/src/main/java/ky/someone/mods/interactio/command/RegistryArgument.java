package ky.someone.mods.interactio.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class RegistryArgument implements ArgumentType<Registry<?>> {

    private final Collection<ResourceLocation> registries = Registry.REGISTRY.keySet();

    private static final SimpleCommandExceptionType BAD_REGISTRY = new SimpleCommandExceptionType(new TextComponent("Registry does not exist"));

    public static <S> Registry<?> getRegistry(CommandContext<S> ctx, String name) {
        return ctx.getArgument(name, Registry.class);
    }

    public static RegistryArgument registry() {
        return new RegistryArgument();
    }

    @Override
    public Registry<?> parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        ResourceLocation resLoc = ResourceLocation.read(reader);

        Registry<?> reg = Registry.REGISTRY.get(resLoc);

        if (reg == null) {
            throw BAD_REGISTRY.createWithContext(reader);
        }

        return reg;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(registries, builder);
    }

}
