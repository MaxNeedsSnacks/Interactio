package ky.someone.mods.interactio.recipe.duration;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

public class DurationManager<R extends DurationRecipe<T,S>, T, S extends StateHolder<?,?>>
{
    protected static Map<Class<? extends DurationRecipe<?,?>>, Map<Level, DurationManager<?,?,?>>> managers = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public static <R extends DurationRecipe<T,S>, T, S extends StateHolder<?,?>> DurationManager<R,T,S> get(Level world, InWorldRecipeType<R> storage, Class<R> cls)
    {
        return (DurationManager<R, T, S>) managers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                                                .computeIfAbsent(world, k -> new DurationManager<>(world, storage, cls));
    }
    
    protected Map<BlockPos, SimpleEntry<R, Integer>> existingRecipes;
    protected RecipeDataTracker<T, S, R> tracker;
    protected InWorldRecipeType<R> storage;
    
    protected DurationManager(Level world, InWorldRecipeType<R> storage, Class<R> cls)
    {
        this.existingRecipes = new HashMap<>();
        this.storage = storage;
        this.tracker = RecipeDataTracker.get(world, cls);
    }
    
    public RecipeDataTracker<T,S,R> getTracker() { return this.tracker; }

    public static void tickAllRecipes(Level world)
    {
        DurationManager.managers.values().stream()
            .map(map -> map.get(world)).filter(Objects::nonNull)
            .forEach(manager -> manager.tickRecipes(world));
    }
    
    public void tickRecipes(Level world)
    {
        List<BlockPos> toRemove = new LinkedList<>();
        this.existingRecipes.forEach((pos, entry) -> {
            T input = tracker.getInput(pos);
            S state = tracker.getState(pos);
            R recipe = entry.getKey();
            int duration = entry.getValue() + 1;
            Interactio.LOGGER.warn("|><|" + duration + "|><|" + input + "|><|" + state);
            if (input == null || state == null)
                toRemove.add(pos);
            else if (recipe.canCraft(world, pos, input, state))
            {
                recipe.tick(input, state);
                entry.setValue(duration);
                if (recipe.isFinished(duration))
                {
                    recipe.craft(input, new DefaultInfo(world, pos));
                    toRemove.add(pos);
                }
                tracker.clear(pos);
            }
            else toRemove.add(pos);
        });
        
        for (BlockPos pos : toRemove)
            this.existingRecipes.remove(pos);
        toRemove.clear();
        
        tracker.forEach((input, state, pos) -> {
            storage.apply(recipe -> recipe.canCraft(world, pos, input, state),
                          recipe -> trackOrCraft(world, pos, recipe, input));
        });
        tracker.clear();
    }
    
    private void trackOrCraft(Level world, BlockPos pos, R recipe, T input)
    {
        if (recipe.getDuration() == 0)
            recipe.craft(input, new DefaultInfo(world, pos));
        else this.existingRecipes.put(pos, new SimpleEntry<>(recipe, 0));
    }
}
