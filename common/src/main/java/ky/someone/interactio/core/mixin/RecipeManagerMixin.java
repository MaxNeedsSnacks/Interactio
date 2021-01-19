package ky.someone.interactio.core.mixin;

import com.google.gson.JsonObject;
import ky.someone.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
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

    // This gets called by ClientPacketListener#handleUpdateRecipes
    // when handling ClientboundUpdateRecipesPacket packets.
    @Inject(method = "replaceRecipes", at = @At("RETURN"))
    public void clearCacheClient(Iterable<Recipe<?>> recipes, CallbackInfo ci) {
        InWorldRecipeType.clearCache();
    }

}
