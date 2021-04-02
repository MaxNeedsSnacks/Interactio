package ky.someone.mods.interactio;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import ky.someone.mods.interactio.recipe.Events.EventType;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.ingredient.WeightedOutput;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public final class Utils {

    private static List<Block> anvils = Arrays.asList(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
    
    public static boolean isAnvil(Block block) {
        return anvils.contains(block);
    }
    
    public static boolean isAnvil(BlockState state) {
        return isAnvil(state.getBlock());
    }
    
    public static boolean isItem(Entity e) {
        return e instanceof ItemEntity;
    }

    // region recipe
    public static boolean compareStacks(List<ItemEntity> entities, Collection<ItemIngredient> ingredients) {
        return compareStacks(entities, new Object2IntOpenHashMap<>(), ingredients);
    }

    public static boolean compareStacks(List<ItemEntity> entities, Object2IntMap<ItemEntity> used, Collection<ItemIngredient> ingredients) {

        Collection<ItemIngredient> required = new ObjectOpenHashSet<>();
        ingredients.forEach(i -> required.add(i.copy()));

        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();

            if (!entity.isAlive()) return false;

            for (ItemIngredient req : required) {
                Ingredient ingredient = req.getIngredient();
                int available = Math.min(req.getCount(), item.getCount());

                if (ingredient.test(item)) {
                    used.mergeInt(entity, available - req.roll(available), Integer::sum);
                    req.shrink(item.getCount());
                    break;
                }
            }

            required.removeIf(ItemIngredient::isEmpty);
        }

        return required.isEmpty();
    }

    public static void shrinkAndUpdate(Object2IntMap<ItemEntity> entities) {
        entities.forEach((entity, count) -> {
            entity.setNeverPickUp();

            ItemStack item = entity.getItem().copy();
            item.shrink(count);

            entity.setItem(item);

            entity.setDefaultPickUpDelay();
        });
    }

    public static <T> WeightedOutput<T> singleOrWeighted(JsonObject json, IEntrySerializer<T> serializer) {
        WeightedOutput<T> output = new WeightedOutput<>(0);
        try {
            output.add(serializer.read(json), 1);
        } catch (Exception e) {
            output = WeightedOutput.deserialize(json, serializer);
        }
        return output;
    }
    //endregion recipe

    // region network
    public static void sendParticle(ParticleOptions particle, Level world, Vec3 pos) {
        sendParticle(particle, world, pos, 5);
    }

    public static void sendParticle(ParticleOptions particle, Level world, Vec3 pos, int count) {
        if (world instanceof ServerLevel) {
            Random rand = world.random;

            double dx = rand.nextGaussian() / 50;
            double dy = rand.nextGaussian() / 50;
            double dz = rand.nextGaussian() / 50;

            ((ServerLevel) world).sendParticles(
                    particle,
                    pos.x - dx,
                    pos.y + Mth.nextDouble(rand, 0, 1 - dy),
                    pos.z - dz,
                    count,
                    dx,
                    dy,
                    dz,
                    rand.nextGaussian() / 50
            );
        }
    }

    public static double parseChance(JsonObject object, String key) {
        return parseChance(object, key, 0);
    }

    public static double parseChance(JsonObject object, String key, double dv) {
        try {
            return getDouble(object, key, dv);
        } catch (Exception ex) {
            return GsonHelper.getAsBoolean(object, key, dv == 1) ? 1 : 0;
        }
    }

    public static double getDouble(JsonObject object, String key) {
        if (object.has(key)) {
            JsonElement e = object.get(key);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                return e.getAsDouble();
            } else {
                throw new JsonSyntaxException("Could not parse double from " + key + " as it's not a number!");
            }
        } else {
            throw new JsonSyntaxException("Missing " + key + ", expected to find a Double");
        }
    }

    public static double getDouble(JsonObject object, String key, double dv) {
        return object.has(key) ? getDouble(object, key) : dv;
    }

    // shouldn't be needed, but who knows
    public static void ensureClientSide(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            throw new UnsupportedOperationException("Packet should only be handled on client!");
        }
    }
    // endregion network

    public static Component translate(String langKey, @Nullable Style style, Object... replacements) {
        return new TranslatableComponent(langKey, replacements).setStyle(style == null ? Style.EMPTY : style);
    }

    public static Component formatChance(double chance, ChatFormatting... styles) {
        return new TextComponent(String.format("%.2f%%", chance * 100.0)).withStyle(styles);
    }

    /**
     * NOTE: This method originally stems from the Botania mod by Vazkii, which is Open Source
     * and distributed under the Botania License (see http://botaniamod.net/license.php)
     * <p>
     * Find the original Botania GitHub repository here: https://github.com/Vazkii/Botania
     * <p>
     * (Original class: vazkii.botania.client.integration.jei.petalapothecary.PetalApothecaryRecipeCategory, created by <williewillus>)
     */
    public static Point rotatePointAbout(Point in, Point about, double degrees) {
        double rad = degrees * Math.PI / 180.0;
        double newX = Math.cos(rad) * (in.x - about.x) - Math.sin(rad) * (in.y - about.y) + about.x;
        double newY = Math.sin(rad) * (in.x - about.x) + Math.cos(rad) * (in.y - about.y) + about.y;
        return new Point((int) Math.round(newX), (int) Math.round(newY));
    }
    
    public static <T, U> void runAll(Map<RecipeEvent<T, U>, JsonObject> events, T t, U u) {
        events.forEach((event, json) -> event.accept(t, u, json));
    }
    
    public static <T, U, V> void runAll(Map<RecipeTickEvent<T, U>, JsonObject> events, T t, U u, DefaultInfo info) {
        events.forEach((event, json) -> event.accept(t, u, info, json));
    }
    
    public static <T, U, V> boolean testAll(Map<RecipeStartPredicate<T, U, V>, JsonObject> events, T t, U u, V v) {
        return events.entrySet().stream().map(entry -> entry.getKey().test(t, u, v, entry.getValue())).reduce(true, (a,b) -> a && b);
    }
    
    public static <T, U, V> boolean testAll(Map<RecipeContinuePredicate<T, U>, JsonObject> events, T t, U u) {
        return events.entrySet().stream().map(entry -> entry.getKey().test(t, u, entry.getValue())).reduce(true, (a,b) -> a && b);
    }
    
    @FunctionalInterface public interface RecipeStartPredicate<T,U,V> { public boolean test(T t, U u, V v, JsonObject json); }
    @FunctionalInterface public interface RecipeContinuePredicate<T,U> { public boolean test(T t, U u, JsonObject json); }
    @FunctionalInterface public interface RecipeEvent<T,U> { public void accept(T t, U u, JsonObject json); }
    @FunctionalInterface public interface RecipeTickEvent<T,U> { public void accept(T t, U u, DefaultInfo info, JsonObject json); }
    @FunctionalInterface public interface TriConsumer<T,U,V> { public void accept(T t, U u, V v); }
    
    public static JsonObject getData(EventType type, ResourceLocation loc, JsonObject json) {
        if (!json.has(type.jsonName)) return null;
        JsonArray array = GsonHelper.getAsJsonArray(json, type.jsonName);
        return Streams.stream(array.iterator())
               .filter(JsonElement::isJsonObject)
               .map(JsonElement::getAsJsonObject)
               .filter(obj -> obj.has("type"))
               .filter(obj -> GsonHelper.isStringValue(obj, "type"))
               .filter(obj -> 
                   new ResourceLocation(GsonHelper.getAsString(obj, "type")).equals(loc)
               ).findFirst().orElse(null);
    }
    
    public static JsonObject getData(EventType[] types, ResourceLocation loc, JsonObject json) {
        return Arrays.stream(types).map(type -> getData(type, loc, json)).filter(Objects::nonNull).findFirst().orElse(null);
    }
}
