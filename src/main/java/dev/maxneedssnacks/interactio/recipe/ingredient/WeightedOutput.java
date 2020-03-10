package dev.maxneedssnacks.interactio.recipe.ingredient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.IEntrySerializer;
import lombok.Getter;
import lombok.Value;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class WeightedOutput<E> extends LinkedHashSet<WeightedOutput.WeightedEntry<E>> {

    @Getter
    public final Random random;

    @Getter
    public final double emptyWeight;

    @Getter
    public final int rolls;

    @Getter
    public final boolean unique;

    private double totalWeight = 0.0;

    private final NavigableMap<Double, WeightedEntry<E>> chanceMap = new TreeMap<>();

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
        if (entry == null || entry.getResult() == null) return false;
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
                .filter(e -> e != null && e.getResult() != null)
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
    // 0.4 - 0.5 apple
    // 0.5 - 0.7 orange
    // 0.7 - 1 tomato
    private void updateChances() {
        chanceMap.clear();
        totalWeight = totalWeight();
        double acc = emptyWeight / totalWeight;
        for (WeightedEntry<E> entry : this) {
            chanceMap.put(acc, entry);
            acc += entry.getWeight() / totalWeight;
        }
    }

    @Nullable
    public E rollOnce() {
        double d = random.nextDouble();
        Map.Entry<Double, WeightedEntry<E>> ceil = chanceMap.ceilingEntry(d);
        if (ceil == null) return null;
        return ceil.getValue().getResult();
    }

    @Nullable
    public E rollFiltered(Predicate<WeightedEntry<E>> filter) {
        WeightedOutput<E> filtered = new WeightedOutput<>(this.random, this.emptyWeight, this.rolls, unique);
        filtered.addAll(stream().filter(filter).collect(Collectors.toSet()));
        return filtered.rollOnce();
    }

    public Collection<E> roll() {
        Set<E> results = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            results.add(unique ? rollFiltered(e -> !results.contains(e.getResult())) : rollOnce());
        }
        results.removeIf(Objects::isNull);
        return results;
    }

    public static <E> WeightedOutput<E> deserialize(JsonObject json, IEntrySerializer<E> serializer) {
        double emptyWeight = Utils.getDouble(json, "empty_weight", 0);
        int rolls = JSONUtils.getInt(json, "rolls", 1);
        boolean unique = JSONUtils.getBoolean(json, "unique", false);

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

    public static <E> WeightedOutput<E> read(PacketBuffer buffer, IEntrySerializer<E> serializer) {
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

    public void write(PacketBuffer buffer, IEntrySerializer<E> serializer) {
        buffer.writeDouble(emptyWeight);
        buffer.writeVarInt(rolls);
        buffer.writeBoolean(unique);

        buffer.writeVarInt(size());
        forEach(entry -> {
            serializer.write(buffer, entry.getResult());
            buffer.writeDouble(entry.getWeight());
        });
    }

    @Value
    public static class WeightedEntry<T> {
        T result;
        double weight;
    }

}