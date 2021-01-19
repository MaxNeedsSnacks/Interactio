package ky.someone.interactio.fabric;

import ky.someone.interactio.Interactio;
import net.fabricmc.api.ModInitializer;

// TODO: i really wish we didn't need this (apparently), shouldn't a method reference be good enough?
public class InteractioFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Interactio.LOGGER.info("HELP ME PLEASE I DON'T WANT TO DIE");
        Interactio.init();
    }
}
