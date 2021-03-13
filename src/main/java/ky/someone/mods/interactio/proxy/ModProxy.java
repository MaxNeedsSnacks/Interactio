package ky.someone.mods.interactio.proxy;

import javax.annotation.Nullable;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.command.CommandItemInfo;
import ky.someone.mods.interactio.command.CommandRegistryDump;
import ky.someone.mods.interactio.command.RegistryArgument;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

public abstract class ModProxy implements IProxy {

    private MinecraftServer server = null;

    public ModProxy() {

        // init methods
        InWorldRecipeType.init();

        // Forge Event Bus events
        MinecraftForge.EVENT_BUS.addListener((FMLServerAboutToStartEvent event) -> this.server = event.getServer());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        Interactio.MOD_BUS.addListener(this::preInit);

    }

    private void preInit(FMLCommonSetupEvent event) {
        ArgumentTypes.register("interactio:registry", RegistryArgument.class, new EmptyArgumentSerializer<>(RegistryArgument::registry));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandItemInfo.register(event.getDispatcher());
        CommandRegistryDump.register(event.getDispatcher());
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public String getVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public RecipeManager getRecipeManager() {
        return getWorld() == null ? null : getWorld().getRecipeManager();
    }

    public static class Client extends ModProxy {
        public Client() {
        }

        @Nullable
        @Override
        public ClientLevel getWorld() {
            return Minecraft.getInstance().level;
        }
    }

    public static class Server extends ModProxy {
        public Server() {
        }

        @Nullable
        @Override
        public ServerLevel getWorld() {
            return getServer() == null ? null : getServer().getLevel(Level.OVERWORLD);
        }
    }
}
