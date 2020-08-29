package dev.maxneedssnacks.interactio;

import dev.maxneedssnacks.interactio.event.ExplosionHandler;
import dev.maxneedssnacks.interactio.proxy.IProxy;
import dev.maxneedssnacks.interactio.proxy.ModProxy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.UUID;


@Mod(Interactio.MOD_ID)
public class Interactio {

    public static final String MOD_ID = "interactio";

    public static final Logger LOGGER = LogManager.getLogger();

    public static IProxy PROXY;
    public static Interactio INSTANCE;

    public static UUID CHAT_ID = UUID.randomUUID();

    private final String PROTOCOL_VERSION = "1";

    public Interactio() {

        // static base variables
        INSTANCE = this;
        PROXY = DistExecutor.safeRunForDist(() -> ModProxy.Client::new, () -> ModProxy.Server::new);

        // static event handlers
        MinecraftForge.EVENT_BUS.register(ExplosionHandler.class);

        // non-static event handlers

    }

    public static String getVersion() {
        Optional<? extends ModContainer> o = ModList.get().getModContainerById(MOD_ID);
        return o.isPresent() ? o.get().getModInfo().getVersion().toString() : "0.0.0";
    }

    public static boolean isDev() {
        return getVersion().equals("0.0.0");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

}
