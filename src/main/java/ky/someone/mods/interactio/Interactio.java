package ky.someone.mods.interactio;

import ky.someone.mods.interactio.event.InteractioEventHandler;
import ky.someone.mods.interactio.proxy.IProxy;
import ky.someone.mods.interactio.proxy.ModProxy;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import me.shedaniel.architectury.event.events.RecipeUpdateEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;


@Mod(Interactio.MOD_ID)
public class Interactio {

    public static final String MOD_ID = "interactio";

    public static final Logger LOGGER = LogManager.getLogger();

    public static Interactio INSTANCE;
    public static IEventBus MOD_BUS;
    public static IProxy PROXY;

    public static UUID CHAT_ID = UUID.randomUUID();

    public Interactio() {

        // static base variables
        INSTANCE = this;
        MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        PROXY = DistExecutor.safeRunForDist(() -> ModProxy.Client::new, () -> ModProxy.Server::new);

        InteractioEventHandler.init();

        RecipeUpdateEvent.EVENT.register((rm) -> InWorldRecipeType.clearCache());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

}
