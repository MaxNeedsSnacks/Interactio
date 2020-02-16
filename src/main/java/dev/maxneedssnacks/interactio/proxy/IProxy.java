package dev.maxneedssnacks.interactio.proxy;

import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public interface IProxy {

    String getVersion();

    @Nullable
    MinecraftServer getServer();

    @Nullable
    RecipeManager getRecipeManager();

}
