package ky.someone.mods.interactio.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ky.someone.mods.interactio.Interactio;
import ky.someone.mods.interactio.Utils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

public class ItemIngredient extends RecipeIngredient<ItemStack> {

    public static final ItemIngredient EMPTY = new ItemIngredient(Ingredient.EMPTY, 0);

    protected final Ingredient ingredient;

    // technically, i *could* just use the mc world's random here
    // but it doesn't really matter too much
    private static final Random r = new Random();

    public ItemIngredient(Ingredient ingredient, int count) {
        this(ingredient, count, 0);
    }

    public ItemIngredient(Ingredient ingredient, int count, double returnChance) {
        super(count, returnChance);
        this.ingredient = ingredient;

        this.updateEmpty();
    }

    @Override
    protected void updateEmpty() {
        isEmpty = this == EMPTY || this.ingredient == Ingredient.EMPTY || this.count <= 0;
    }

    public static ItemIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();

                Ingredient ingredient = obj.has("ingredient") ? Ingredient.fromJson(obj.get("ingredient")) : Ingredient.fromJson(obj);
                int count = GsonHelper.getAsInt(obj, "count", 1);
                double returnChance = Utils.parseChance(obj, "return_chance");
                return new ItemIngredient(ingredient, count, returnChance);
            } else {
                Ingredient ingredient = Ingredient.fromJson(json);
                return new ItemIngredient(ingredient, 1, 0);
            }
        } else {
            throw new JsonSyntaxException("Ingredient stack cannot be null!");
        }
    }

    public static ItemIngredient read(FriendlyByteBuf buffer) {
        Ingredient ingredient = Ingredient.fromNetwork(buffer);
        int count = buffer.readVarInt();
        double consumeChance = buffer.readDouble();
        return new ItemIngredient(ingredient, count, consumeChance);
    }

    public void write(FriendlyByteBuf buffer) {
        ingredient.toNetwork(buffer);
        buffer.writeVarInt(count);
        buffer.writeDouble(returnChance);
    }

    @Override
    public boolean test(ItemStack stack) {
        return ingredient.test(stack);
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

    public ItemIngredient copy() {
        return new ItemIngredient(ingredient, count, returnChance);
    }

    public boolean roll() {
        return r.nextDouble() <= returnChance;
    }

    public int roll(int times) {
        return (int) IntStream.range(0, times).filter(i -> roll()).count();
    }

    @Override
    public Collection<ItemStack> getMatching() {
        return Arrays.asList((isEmpty ? Ingredient.EMPTY : ingredient).getItems());
    }
}
