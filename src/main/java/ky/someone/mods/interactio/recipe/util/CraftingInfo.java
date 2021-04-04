package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class CraftingInfo {
    protected final InWorldRecipe<?, ?, ?> recipe;
    protected final Level world;

    public CraftingInfo(InWorldRecipe<?, ?, ?> recipe, Level world) {
        this.recipe = recipe;
        this.world = world;
    }

    public InWorldRecipe<?, ?, ?> getRecipe() {
        return recipe;
    }

    public Level getWorld() {
        return world;
    }

    public JsonObject getJson() {
        return recipe.getJson();
    }

    public abstract Vec3 getPos();

    public abstract BlockPos getBlockPos();
}
