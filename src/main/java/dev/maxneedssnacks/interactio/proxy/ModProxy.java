package dev.maxneedssnacks.interactio.proxy;

import dev.maxneedssnacks.interactio.command.CommandItemInfo;
import dev.maxneedssnacks.interactio.command.CommandRegistryDump;
import dev.maxneedssnacks.interactio.network.PacketCraftingParticle;
import dev.maxneedssnacks.interactio.network.PacketInvalidateCache;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;

import static dev.maxneedssnacks.interactio.Interactio.NETWORK;

public abstract class ModProxy implements IProxy {

    private MinecraftServer server = null;

    public ModProxy() {

        // Mod Event Bus events
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        // init methods
        InWorldRecipeType.registerTypes();

        // Forge Event Bus events
        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
    }

    @SuppressWarnings("UnusedAssignment")
    private void commonSetup(FMLCommonSetupEvent event) {

        // init packet handler for particles (and maybe more in the future)
        int id = 0;

        NETWORK.registerMessage(id++, PacketCraftingParticle.class, PacketCraftingParticle::write, PacketCraftingParticle::read, PacketCraftingParticle.Handler::handle);
        NETWORK.registerMessage(id++, PacketInvalidateCache.class, PacketInvalidateCache::write, PacketInvalidateCache::read, PacketInvalidateCache.Handler::handle);

    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        server.getResourceManager().addReloadListener((ISelectiveResourceReloadListener)
                (resourceManager, resourceType) -> {
                    InWorldRecipeType.clearCache();
                    NETWORK.send(PacketDistributor.ALL.noArg(), new PacketInvalidateCache(true));
                }
        );
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
