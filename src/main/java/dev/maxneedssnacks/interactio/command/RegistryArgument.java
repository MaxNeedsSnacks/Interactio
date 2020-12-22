package dev.maxneedssnacks.interactio.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegistryArgument implements ArgumentType<ForgeRegistry<?>> {

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
