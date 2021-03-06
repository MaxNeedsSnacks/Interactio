package ky.someone.mods.interactio.recipe.duration;

import ky.someone.mods.interactio.Utils.TriConsumer;
import ky.someone.mods.interactio.recipe.base.DurationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public class RecipeDataTracker<T, S extends StateHolder<?, ?>, R extends DurationRecipe<T, S>> {
    protected static Map<Class<? extends DurationRecipe<?, ?>>, Map<Level, RecipeDataTracker<?, ?, ?>>> trackers = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected static <T, S extends StateHolder<?, ?>, R extends DurationRecipe<T, S>> RecipeDataTracker<T, S, R> get(Level world, Class<R> cls) {
        return (RecipeDataTracker<T, S, R>) trackers.computeIfAbsent(cls, k -> new WeakHashMap<>())
                .computeIfAbsent(world, k -> new RecipeDataTracker<>());
    }

    protected Map<BlockPos, T> inputs;
    protected Map<BlockPos, S> states;

    protected RecipeDataTracker() {
        this.inputs = new HashMap<>();
        this.states = new HashMap<>();
    }

    @Nullable
    public T getInput(BlockPos pos) {
        return inputs.get(pos);
    }

    public T getInput(BlockPos pos, Supplier<T> defaultGenerator) {
        return inputs.computeIfAbsent(pos, k -> defaultGenerator.get());
    }

    public void setInput(BlockPos pos, T input) {
        inputs.put(pos, input);
    }

    @Nullable
    public S getState(BlockPos pos) {
        return states.get(pos);
    }

    public void setState(BlockPos pos, S newState) {
        states.put(pos, newState);
    }

    public void clear(BlockPos pos) {
        this.inputs.remove(pos);
        this.states.remove(pos);
    }

    public void clear() {
        this.inputs.clear();
        this.states.clear();
    }

    public void forEach(TriConsumer<T, S, BlockPos> consumer) {
        for (BlockPos pos : inputs.keySet()) {
            if (inputs.get(pos) == null || states.get(pos) == null) continue;
            consumer.accept(inputs.get(pos), states.get(pos), pos);
        }
    }
}
