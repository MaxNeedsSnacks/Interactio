package dev.maxneedssnacks.interactio.core.mixin;

import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @Inject(method = "apply", at = @At("RETURN"))
    public void clearCache(Map<ResourceLocation, JsonObject> map, IResourceManager resourceManager, IProfiler profiler, CallbackInfo ci) {
        InWorldRecipeType.clearCache();
    }

    // This gets called by IClientPlayNetHandler#handleUpdateRecipes
    // when handling SUpdateRecipesPacket packets.
    @Inject(method = "deserializeRecipes", at = @At("RETURN"), remap = false)
    public void clearCacheClient(Iterable<IRecipe<?>> recipes, CallbackInfo ci) {
        InWorldRecipeType.clearCache();
    }

}
