package ky.someone.mods.interactio.forge;

import ky.someone.mods.interactio.Interactio;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Interactio.MOD_ID)
public class InteractioForge {

    public InteractioForge() {
        EventBuses.registerModEventBus(Interactio.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        new Interactio();
    }

}
