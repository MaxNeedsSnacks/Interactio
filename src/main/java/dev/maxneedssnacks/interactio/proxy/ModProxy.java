package dev.maxneedssnacks.interactio.proxy;

import dev.maxneedssnacks.interactio.command.CommandItemInfo;
import dev.maxneedssnacks.interactio.command.CommandRegistryDump;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

public abstract class ModProxy implements IProxy {

    private MinecraftServer server = null;

    public ModProxy() {

        // init methods
        InWorldRecipeType.registerTypes();

        // Forge Event Bus events
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);

    }

    private void serverStarting(FMLServerStartingEvent event) {
        CommandItemInfo.register(event.getCommandDispatcher());
        CommandRegistryDump.register(event.getCommandDispatcher());
    }

    @Override
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public String getVersion() {
        return SharedConstants.getVersion().getName();
    }

    public static class Client extends ModProxy {
        public Client() {
        }

        @Override
        public RecipeManager getRecipeManager() {
            return getClientWorld() == null ?
                    (getServer() == null ?
                            null : getServer().getRecipeManager())
                    : getClientWorld().getRecipeManager();
        }

        @Override
        public World getClientWorld() {
            return Minecraft.getInstance().world;
        }
    }

    public static class Server extends ModProxy {
        public Server() {
        }

        @Override
        public RecipeManager getRecipeManager() {
            return getServer() == null ? null : getServer().getRecipeManager();
        }

        @Override
        public World getClientWorld() {
            throw new UnsupportedOperationException("Attempted to call client-side method getClientWorld on server, this is not good!");
        }
    }
}
