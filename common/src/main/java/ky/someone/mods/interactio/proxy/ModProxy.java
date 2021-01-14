package ky.someone.mods.interactio.proxy;

import com.mojang.brigadier.CommandDispatcher;
import ky.someone.mods.interactio.command.CommandItemInfo;
import ky.someone.mods.interactio.command.CommandRegistryDump;
import ky.someone.mods.interactio.command.RegistryArgument;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import me.shedaniel.architectury.event.events.CommandRegistrationEvent;
import me.shedaniel.architectury.event.events.LifecycleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class ModProxy implements Proxy {

    private MinecraftServer server = null;

    public ModProxy() {

        // init methods
        InWorldRecipeType.init();

        // Forge Event Bus events
        LifecycleEvent.SERVER_BEFORE_START.register(this::setServer);
        CommandRegistrationEvent.EVENT.register(this::registerCommands);

        addArgumentTypes();

    }

    private void setServer(MinecraftServer server) {
        this.server = server;
    }

    private void addArgumentTypes() {
        ArgumentTypes.register("interactio:registry", RegistryArgument.class, new EmptyArgumentSerializer<>(RegistryArgument::registry));
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection sel) {
        CommandItemInfo.register(dispatcher);
        CommandRegistryDump.register(dispatcher);
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return getLevel() == null ? null : getLevel().getRecipeManager();
    }

    public static class Client extends ModProxy {
        public Client() {
        }

        @Nullable
        @Override
        public ClientLevel getLevel() {
            return Minecraft.getInstance().level;
        }
    }

    public static class Server extends ModProxy {
        public Server() {
        }

        @Nullable
        @Override
        public ServerLevel getLevel() {
            return getServer() == null ? null : getServer().getLevel(Level.OVERWORLD);
        }
    }
}
