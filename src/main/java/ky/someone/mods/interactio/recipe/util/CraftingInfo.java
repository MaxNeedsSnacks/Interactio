package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public abstract class CraftingInfo {
    protected final Level world;
    protected final JsonObject json;
    
    public CraftingInfo(Level world, JsonObject json)
    {
        this.world = world;
        this.json = json;
    }
    
    public Level getWorld() { return world; }
    
    public JsonObject getJson() { return json; }
    
    public abstract BlockPos getPos();
}
