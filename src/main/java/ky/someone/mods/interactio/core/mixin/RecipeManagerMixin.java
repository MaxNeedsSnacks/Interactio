package ky.someone.mods.interactio.core.mixin;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    public void clearCache(Map<ResourceLocation, JsonObject> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        InWorldRecipeType.clearCache();
    }
}
