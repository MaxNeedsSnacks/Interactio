package dev.maxneedssnacks.interactio.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.Utils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class RecipeIngredient implements Predicate<ItemStack> {

    public static final RecipeIngredient EMPTY = new RecipeIngredient(Ingredient.EMPTY, 0);

    private boolean isEmpty;

    private final Ingredient ingredient;
    private int count;
    private final double returnChance;

    // technically, i *could* just use the mc world's random here
    // but it doesn't really matter too much
    private static final Random r = new Random();

    public RecipeIngredient(Ingredient ingredient, int count) {
        this(ingredient, count, 0);
    }

    public RecipeIngredient(Ingredient ingredient, int count, double returnChance) {
        this.ingredient = ingredient;
        this.count = count;
        this.returnChance = returnChance;

        this.updateEmpty();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    private void updateEmpty() {
        isEmpty = this == EMPTY || this.ingredient == Ingredient.EMPTY || this.count <= 0;
    }

    public static RecipeIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            Ingredient ingredient = Ingredient.deserialize(json);
            int count = 1;
            double returnChance = 0;
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();

                count = JSONUtils.getInt(obj, "count", 1);
                returnChance = Utils.parseChance(obj, "return_chance");
            }
            return new RecipeIngredient(ingredient, count, returnChance);
        } else {
            throw new JsonSyntaxException("Ingredient stack cannot be null!");
        }

    }

    public static RecipeIngredient read(PacketBuffer buffer) {
        Ingredient ingredient = Ingredient.read(buffer);
        int count = buffer.readVarInt();
        double consumeChance = buffer.readDouble();
        return new RecipeIngredient(ingredient, count, consumeChance);
    }

    public void write(PacketBuffer buffer) {
        ingredient.write(buffer);
        buffer.writeVarInt(count);
        buffer.writeDouble(returnChance);
    }

    @Override
    public boolean test(ItemStack stack) {
        return ingredient.test(stack);
    }

    public List<ItemStack> getMatchingStacks() {
        return Arrays.asList(ingredient.getMatchingStacks());
    }

    public Ingredient getIngredient() {
        return isEmpty ? Ingredient.EMPTY : ingredient;
    }

    public int getCount() {
        return isEmpty ? 0 : count;
    }

    public double getReturnChance() {
        return isEmpty ? 0 : returnChance;
    }

    public void setCount(int count) {
        if (isEmpty) {
            Interactio.LOGGER.warn("Attempting to modify an empty ingredient, this is not allowed!");
            return;
        }
        this.count = count;
        updateEmpty();
    }

    public void grow(int amount) {
        this.setCount(this.count + amount);
    }

    public void shrink(int amount) {
        this.grow(-amount);
    }

    public RecipeIngredient copy() {
        return new RecipeIngredient(ingredient, count, returnChance);
    }

    public boolean roll() {
        return r.nextFloat() <= returnChance;
    }

    public int roll(int times) {
        int count = 0;
        for (int i = 0; i < times; i++) {
            count += roll() ? 1 : 0;
        }
        return count;
    }
}
