package ky.someone.mods.interactio.recipe.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public abstract class CraftingInfo {
    protected final Level world;
    
    public CraftingInfo(Level world)
    {
        this.world = world;
    }
    
    public Level getWorld() { return world; }
    
    public abstract BlockPos getPos();
}
