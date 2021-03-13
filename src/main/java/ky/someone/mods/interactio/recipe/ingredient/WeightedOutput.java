package ky.someone.mods.interactio.recipe.ingredient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput.WeightedEntry;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;

@SuppressWarnings("serial")
public class WeightedOutput<E> extends LinkedHashSet<WeightedEntry<E>> {

    public final Random random;

    public final double emptyWeight;

    public final int rolls;

    public final boolean unique;

    private double totalWeight = 0.0;

    private final NavigableMap<Double, E> ranges = new TreeMap<>();

    public WeightedOutput(double emptyWeight) {
        this(new Random(), emptyWeight);
    }

    public WeightedOutput(Random random, double emptyWeight) {
        this(random, emptyWeight, 1);
    }

    public WeightedOutput(Random random, double emptyWeight, int rolls) {
        this(random, emptyWeight, rolls, false);
    }

    public WeightedOutput(Random random, double emptyWeight, int rolls, boolean unique) {
        this.random = random;
        this.emptyWeight = Math.max(emptyWeight, 0);
        this.rolls = Math.max(rolls, 1);
        this.unique = unique;
    }

    public boolean add(E e, double weight) {
        return add(new WeightedEntry<>(e, weight));
    }

    @Override
    public boolean add(WeightedEntry<E> entry) {
        if (entry == null) return false;
        boolean success = super.add(entry);
        updateChances();
        return success;
    }

    @Override
    public boolean remove(Object entry) {
        boolean success = super.remove(entry);
        updateChances();
        return success;
    }

    @Override
    public boolean addAll(Collection<? extends WeightedEntry<E>> c) {
        boolean modified = c.stream()
                .filter(Objects::nonNull)
                .map(super::add) // avoid unnecessarily updating chances by using super
                .reduce(false, Boolean::logicalOr);
        updateChances();
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = c.stream()
                .map(super::remove) // avoid unnecessarily updating chances by using super
                .reduce(false, Boolean::logicalOr);
        updateChances();
        return modified;
    }

    public double getChance(WeightedEntry<E> entry) {
        return entry.getWeight() / totalWeight;
    }

    public double totalWeight() {
        return stream().mapToDouble(WeightedEntry::getWeight).sum() + emptyWeight;
    }

    public boolean isSingle() {
        return size() == 1 && emptyWeight == 0;
    }

    // 0.0 - 0.4 empty
    // 0.4 - 0.7 tomato
    // 0.7 - 0.9 orange
    // 0.9 - 1 apple
    private void updateChances() {
        ranges.clear();
        ranges.put(0.0, null);

        totalWeight = totalWeight();
        double acc = emptyWeight / totalWeight;
        for (WeightedEntry<E> entry : this) {
            ranges.put(acc, entry.getResult());
            acc += entry.getWeight() / totalWeight;
        }
    }

    @Nullable
    public E rollOnce() {
        double d = random.nextDouble();
        Map.Entry<Double, E> match = ranges.floorEntry(d);
        return match.getValue();
    }

    @Nullable
    public E rollFiltered(Predicate<WeightedEntry<E>> filter) {
        WeightedOutput<E> filtered = new WeightedOutput<>(this.random, this.emptyWeight, this.rolls, unique);
        filtered.addAll(stream().filter(filter).collect(Collectors.toSet()));
        return filtered.rollOnce();
    }

    public Collection<E> roll() {
        Collection<E> results = new ArrayList<>();
        for (int i = 0; i < rolls; i++) {
            results.add(unique ? rollFiltered(e -> !results.contains(e.getResult())) : rollOnce());
        }
        results.removeIf(Objects::isNull);
        return results;
    }

    public static <E> WeightedOutput<E> deserialize(JsonObject json, IEntrySerializer<E> serializer) {
        double emptyWeight = Utils.getDouble(json, "empty_weight", 0);
        int rolls = GsonHelper.getAsInt(json, "rolls", 1);
        boolean unique = GsonHelper.getAsBoolean(json, "unique", false);

        WeightedOutput<E> output = new WeightedOutput<>(new Random(), emptyWeight, rolls, unique);

        JsonElement el = json.get("entries");
        JsonArray arr;
        if (el.isJsonArray()) {
            arr = el.getAsJsonArray();
        } else {
            arr = new JsonArray();
            arr.add(el);
        }

        StreamSupport.stream(arr.spliterator(), false)
                .filter(e -> e instanceof JsonObject)
                .map(JsonElement::getAsJsonObject)
                .forEach(entry -> {
                    E result = serializer.read(entry.getAsJsonObject("result"));
                    double weight = Utils.getDouble(entry, "weight");
                    output.add(result, weight);
                });

        return output;
    }

    public static <E> WeightedOutput<E> read(FriendlyByteBuf buffer, IEntrySerializer<E> serializer) {
        double emptyWeight = buffer.readDouble();
        int rolls = buffer.readVarInt();
        boolean unique = buffer.readBoolean();

        WeightedOutput<E> output = new WeightedOutput<>(new Random(), emptyWeight, rolls, unique);

        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            E result = serializer.read(buffer);
            double weight = buffer.readDouble();
            output.add(result, weight);
        }

        return output;
    }

    public void write(FriendlyByteBuf buffer, IEntrySerializer<E> serializer) {
        buffer.writeDouble(emptyWeight);
        buffer.writeVarInt(rolls);
        buffer.writeBoolean(unique);

        buffer.writeVarInt(size());
        forEach(entry -> {
            serializer.write(buffer, entry.getResult());
            buffer.writeDouble(entry.getWeight());
        });
    }

    public static final class WeightedEntry<T> {
        private final T result;
        private final double weight;

        public WeightedEntry(T result, double weight) {
            this.result = result;
            this.weight = weight;
        }

        public T getResult() {
            return this.result;
        }

        public double getWeight() {
            return this.weight;
        }

        public String toString() {
            return "WeightedOutput.WeightedEntry(result=" + this.getResult() + ", weight=" + this.getWeight() + ")";
        }
    }

}