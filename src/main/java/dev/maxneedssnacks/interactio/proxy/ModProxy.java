package dev.maxneedssnacks.interactio.proxy;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.command.CommandItemInfo;
import dev.maxneedssnacks.interactio.command.CommandRegistryDump;
import dev.maxneedssnacks.interactio.network.PacketCraftingParticle;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;

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

    private void commonSetup(FMLCommonSetupEvent event) {

        // init packet handler for particles (and maybe more in the future)
        int id = 0;

        // noinspection UnusedAssignment
        Interactio.NETWORK.registerMessage(id++, PacketCraftingParticle.class, PacketCraftingParticle::write, PacketCraftingParticle::read, PacketCraftingParticle.Handler::handle);

    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        server.getResourceManager().addReloadListener(
                new JsonReloadListener(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(), "recipes") {
                    @Override
                    protected void apply(Map<ResourceLocation, JsonObject> _0, IResourceManager _1, IProfiler _2) {
                        InWorldRecipeType.clearCache();
                    }
                });
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
    public RecipeManager getRecipeManager() {
        return server == null ? null : server.getRecipeManager();
    }

    @Override
    public String getVersion() {
        return SharedConstants.getVersion().getName();
    }

    public static class Client extends ModProxy {
        public Client() {
        }
    }

    public static class Server extends ModProxy {
        public Server() {
        }
    }
}
