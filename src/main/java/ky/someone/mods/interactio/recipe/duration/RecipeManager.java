package ky.someone.mods.interactio.recipe.duration;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

public class RecipeManager<R extends DurationRecipe<T,S>, T, S extends StateHolder<?,?>>
{
    protected static Map<Class<? extends DurationRecipe<?,?>>, Map<Level, RecipeManager<?,?,?>>> managers = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public static <R extends DurationRecipe<T,S>, T, S extends StateHolder<?,?>> RecipeManager<R,T,S> get(Level world, Class<R> cls)
    {
        return (RecipeManager<R, T, S>) managers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                                                .computeIfAbsent(world, k -> new RecipeManager<>(world, cls));
    }
    
    protected Map<BlockPos, SimpleEntry<R, Integer>> existingRecipes;
    protected RecipeTracker<T, S, R> tracker;
    protected InWorldRecipeType<R> storage;
    
    protected RecipeManager(Level world, Class<R> cls)
    {
        this.existingRecipes = new HashMap<>();
        this.tracker = RecipeTracker.get(world, cls);
    }

    public static void tickAllRecipes(Level world)
    {
        RecipeManager.managers.values().stream()
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
            int duration = entry.getValue();
            if (recipe.canCraft(world, input, state))
            {
                recipe.tick(input, state);
                duration++;
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
            storage.apply(recipe -> recipe.canCraft(world, input, state),
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
