package ky.someone.mods.interactio.proxy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface Proxy {

    @Nullable
    MinecraftServer getServer();

    @Nullable
    RecipeManager getRecipeManager();

    @Nullable
    Level getLevel();

}
