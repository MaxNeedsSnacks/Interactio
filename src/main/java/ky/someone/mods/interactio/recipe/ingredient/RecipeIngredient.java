package ky.someone.mods.interactio.recipe.ingredient;

import java.util.Collection;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import net.minecraft.network.FriendlyByteBuf;

public abstract class RecipeIngredient<T> implements Predicate<T>
{
    protected boolean isEmpty;
    
    protected int count;
    protected final double returnChance;
    
    protected final Random r = new Random();
    
    public RecipeIngredient(int count, double returnChance)
    {
        this.count = count;
        this.returnChance = returnChance;
    }
    
    public boolean isEmpty() { return isEmpty; }
    
    protected abstract void updateEmpty();
    
    protected abstract void write(FriendlyByteBuf buffer);
    
    public abstract boolean roll();
    
    public int roll(int times)
    {
        return (int) IntStream.range(0, times)
                .filter(i -> roll())
                .count();
    }
    
    public abstract Collection<T> getMatching();
}
