package dev.maxneedssnacks.interactio.recipe.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;

public class IngredientStack {

    public static final IngredientStack EMPTY = new IngredientStack(Ingredient.EMPTY, 0);

    private boolean isEmpty;

    private final Ingredient ingredient;
    private int count;

    public IngredientStack(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;

        this.updateEmpty();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    private void updateEmpty() {
        isEmpty = this == EMPTY || this.ingredient == Ingredient.EMPTY || this.count <= 0;
    }

    public Ingredient getIngredient() {
        return isEmpty ? Ingredient.EMPTY : ingredient;
    }

    public int getCount() {
        return isEmpty ? 0 : count;
    }

    public void setCount(int count) {
        if (isEmpty) throw new IllegalStateException("Can't modify the empty stack.");
        this.count = count;
        updateEmpty();
    }

    public void grow(int amount) {
        this.setCount(this.count + amount);
    }

    public void shrink(int amount) {
        this.grow(-amount);
    }

    public IngredientStack copy() {
        return new IngredientStack(ingredient, count);
    }

    public static IngredientStack deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            Ingredient ingredient = Ingredient.deserialize(json);
            int count = JSONUtils.getInt(json.getAsJsonObject(), "count", 1);
            return new IngredientStack(ingredient, count);
        } else {
            throw new JsonSyntaxException("Ingredient stack cannot be null!");
        }
    }

    public static IngredientStack read(PacketBuffer buffer) {
        Ingredient ingredient = Ingredient.read(buffer);
        int count = buffer.readVarInt();
        return new IngredientStack(ingredient, count);
    }

    public void write(PacketBuffer buffer) {
        ingredient.write(buffer);
        buffer.writeVarInt(count);
    }
}
