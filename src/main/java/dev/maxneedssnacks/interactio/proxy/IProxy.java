package dev.maxneedssnacks.interactio.proxy;

import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;

public interface IProxy {

    String getVersion();

    MinecraftServer getServer();

    RecipeManager getRecipeManager();

}
