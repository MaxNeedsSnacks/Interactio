package dev.maxneedssnacks.interactio;

import dev.maxneedssnacks.interactio.event.DroppedItemHandler;
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


@Mod(Interactio.MOD_ID)
public class Interactio {

    public static final String MOD_ID = "interactio";

    public static final Logger LOGGER = LogManager.getLogger();

    public static IProxy PROXY;
    public static Interactio INSTANCE;

    private final String PROTOCOL_VERSION = "1";
    public static SimpleChannel NETWORK;

    public Interactio() {

        // static base variables
        INSTANCE = this;
        PROXY = DistExecutor.runForDist(() -> ModProxy.Client::new, () -> ModProxy.Server::new);
        NETWORK = NetworkRegistry.newSimpleChannel(id("particles"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

        // static event handlers
        MinecraftForge.EVENT_BUS.register(ExplosionHandler.class);

        // non-static event handlers
        MinecraftForge.EVENT_BUS.register(new DroppedItemHandler());

        /*
                WeightedOutput<String> x = new WeightedOutput<>(new Random(), 4, 3, true);
                x.add(null);
                x.add(null, 0);
                x.add("tomato", 3);
                x.add("orange", 2);
                x.add("apple", 1);

                int t = 1_000;
                IntStream.range(0, 10)
                        .mapToObj(_0 -> IntStream.range(0, t).boxed().collect(Collectors.toMap(_1 -> Objects.toString(x.rollOnce()), _1 -> 1, Integer::sum)))
                        .peek(TreeMap::new)
                        .forEachOrdered(LOGGER::info);
                LOGGER.info("jhfsdjhfjhkskjdhfskdj");

                for (int i = 0; i < 10; i++) {
                    LOGGER.info(x.roll());
                }

                LOGGER.info("done");
        */
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
