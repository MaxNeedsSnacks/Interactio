package ky.someone.mods.interactio.recipe.duration;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import ky.someone.mods.interactio.Utils.TriConsumer;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

public class RecipeTracker<T, S extends StateHolder<?,?>, R extends DurationRecipe<T, S>>
{
    protected static Map<Class<? extends DurationRecipe<?,?>>, Map<Level, RecipeTracker<?,?,?>>> trackers = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public static <T, S extends StateHolder<?,?>, R extends DurationRecipe<T, S>> RecipeTracker<T, S, R> get(Level world, Class<R> cls)
    {
        return (RecipeTracker<T, S, R>) trackers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                                                .computeIfAbsent(world, k -> new RecipeTracker<>());
    }
    
    protected Map<BlockPos, R> recipes;
    protected Map<BlockPos, T> inputs;
    protected Map<BlockPos, S> states;
    
    protected RecipeTracker()
    {
        this.recipes = new HashMap<>();
        this.inputs = new HashMap<>();
        this.states = new HashMap<>();
    }
    
    @Nullable public T getInput(BlockPos pos) { return inputs.get(pos); }
    public T getInput(BlockPos pos, Supplier<T> defaultGenerator) { return inputs.computeIfAbsent(pos, k -> defaultGenerator.get()); }
    public void setInput(BlockPos pos, T input) { inputs.put(pos, input); }
    @Nullable public S getState(BlockPos pos) { return states.get(pos); }
    public void setState(BlockPos pos, S newState) { states.put(pos, newState); }
    
    public void clear(BlockPos pos)
    {
        this.inputs.remove(pos);
        this.states.remove(pos);
    }
    
    public void clear()
    {
        
        this.inputs.clear();
        this.states.clear();
    }
    
    public void forEach(TriConsumer<T, S, BlockPos> consumer)
    {
        
    }
}
