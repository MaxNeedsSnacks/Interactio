package dev.maxneedssnacks.interactio.proxy;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.command.CommandItemInfo;
import dev.maxneedssnacks.interactio.network.PacketCraftingParticle;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class ModProxy implements IProxy {

    private MinecraftServer server = null;

    public ModProxy() {

        // Mod Event Bus events
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        // init methods
        ModRecipes.init();

        // Forge Event Bus events
        MinecraftForge.EVENT_BUS.addListener(this::serverAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarting);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

        // init packet handler for particles (and maybe more in the future)
        int id = 0;

        // noinspection UnusedAssignment
        Interactio.NETWORK.registerMessage(id++, PacketCraftingParticle.class, PacketCraftingParticle::write, PacketCraftingParticle::read, PacketCraftingParticle.Handler::handle);

    }

    private void serverAboutToStart(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        server.getResourceManager().addReloadListener(
                new JsonReloadListener(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(), "recipes") {
                    @Override
                    protected void apply(Map<ResourceLocation, JsonObject> splashList, IResourceManager resourceManagerIn, IProfiler profilerIn) {
                        Set<Ingredient> allInputs = Sets.newHashSet();

                        Map<ResourceLocation, JsonObject> filtered = Maps.filterKeys(splashList, (loc) -> loc != null && Interactio.MOD_ID.equals(loc.getNamespace()));

                        filtered.forEach((loc, json) -> {
                            try {
                                if (CraftingHelper.processConditions(json, "conditions")) {
                                    IRecipe<?> recipe = RecipeManager.deserializeRecipe(loc, json);
                                    allInputs.addAll(recipe.getIngredients());
                                }
                            } catch (Exception e) {
                                Interactio.LOGGER.debug("Exception caught while reloading datapacks: ", e);
                            }
                        });

                        ModRecipes.ANY_VALID = Ingredient.merge(allInputs);

                        /*
                            ModRecipes.RECIPE_MAP = server.getRecipeManager()
                                    .getRecipes()
                                    .parallelStream()
                                    .filter(Objects::nonNull)
                                    .filter(InWorldRecipe.class::isInstance)
                                    .map(InWorldRecipe.class::cast)
                                    .collect(Multimaps.toMultimap(IRecipe::getType, Function.identity(), ArrayListMultimap::create));
                        */

                        ModRecipes.RECIPE_MAP.clear();

                        server.getRecipeManager().getRecipes()
                                .parallelStream()
                                .filter(Objects::nonNull)
                                .filter(InWorldRecipe.class::isInstance)
                                .map(r -> (InWorldRecipe<?, ?, ?>) r)
                                .forEach(r -> ModRecipes.RECIPE_MAP.put(r.getType(), r));
                    }
                });
    }

    private void serverStarting(FMLServerStartingEvent event) {
        CommandItemInfo.register(event.getCommandDispatcher());
    }

    @Override
    public MinecraftServer getServer() {
        return server;
    }

    @Override
    public RecipeManager getRecipeManager() {
        return server.getRecipeManager();
    }

    @Override
    public String getVersion() {
        return SharedConstants.getVersion().getName();
    }

    public static class Client extends ModProxy {
        public Client() {
        }
    }

    public static class Server extends ModProxy {
        public Server() {
        }
    }
}
