package ky.someone.mods.interactio.recipe.ingredient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * An {@code Ingredient}-equivalent for entitys, based heavily on the vanilla implementation.
 */
public class EntityIngredient extends RecipeIngredient<EntityType<?>> {

    public static final EntityIngredient EMPTY = new EntityIngredient(Stream.empty());

    private final IEntityList[] acceptedEntities;
    private Collection<EntityType<?>> matchingEntities;

    protected EntityIngredient(Stream<? extends IEntityList> entityLists) {
        super(1, 0);
        this.acceptedEntities = entityLists.toArray(IEntityList[]::new);
    }

    /**
     * Get a list of all {@link Entity}s which match this ingredient. Used for JEI support.
     *
     * @return A list of matching entities
     */
    public Collection<EntityType<?>> getMatching() {
        this.determineMatchingEntities();
        return matchingEntities;
    }

    private void determineMatchingEntities() {
        if (this.matchingEntities == null) {
            this.matchingEntities = Arrays.stream(this.acceptedEntities)
                    .flatMap(list -> list.getEntities().stream())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Test for a match.
     *
     * @param entity Entity to check the ingredient against.
     * @return True if the entity matches the ingredient
     */
    public boolean test(@Nullable EntityType<?> entity) {
        if (entity == null) {
            return false;
        } else {
            this.determineMatchingEntities();
            return matchingEntities.contains(entity);
        }
    }

    /**
     * Test for a match using a entity.
     *
     * @param state Entity state to check the ingredient against
     * @return True if the entity matches the ingredient
     */
    public boolean test(@Nullable LivingEntity entity) {
        if (entity == null) return false;
        return test(entity.getType());
    }

    /**
     * Deserialize a {@link EntityIngredient} from JSON.
     *
     * @param json The JSON object
     * @return A new EntityIngredient
     * @throws JsonSyntaxException If the JSON cannot be parsed
     */
    public static EntityIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return new EntityIngredient(Stream.of(deserializeEntityList(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() == 0) {
                    throw new JsonSyntaxException("Array cannot be empty, at least one entity or tag must be defined");
                }
                return new EntityIngredient(StreamSupport.stream(arr.spliterator(), false)
                        .map(element -> deserializeEntityList(element.getAsJsonObject())));
            } else {
                throw new JsonSyntaxException("Expected either an object or an array of objects for entity ingredient");
            }
        }

        throw new JsonSyntaxException("Entity cannot be null");
    }

    public static IEntityList deserializeEntityList(JsonObject json) {
        if (json.has("entity") && json.has("tag")) {
            throw new JsonSyntaxException("Entity ingredient should have either 'tag' or 'entity', not both!");
        } else if (json.has("entity")) {
            EntityType<?> entity = IEntrySerializer.ENTITY.read(json);
            return new SingleEntityList(entity);
//        } else if (json.has("tag")) {
//            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
//            Tag<EntityType<?>> tag = EntityTags.getAllTags().getTag(id);
//            if (tag == null) {
//                throw new JsonSyntaxException("Unknown entity tag '" + id + "'");
//            }
//            return new TagList(tag);
        }

        throw new JsonSyntaxException("Entity ingredient should have either 'tag' or 'entity'");
    }

    /**
     * Reads a {@link EntityIngredient} from a packet buffer. Use with {@link #write(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     * @return A new EntityIngredient
     */
    public static EntityIngredient read(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        return new EntityIngredient(Stream.generate(() -> new SingleEntityList(IEntrySerializer.ENTITY.read(buffer))).limit(size));
    }

    /**
     * Writes the ingredient to a packet buffer. Use with {@link #read(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     */
    public void write(FriendlyByteBuf buffer) {
        this.determineMatchingEntities();
        buffer.writeVarInt(matchingEntities.size());
        matchingEntities.forEach(entity -> IEntrySerializer.ENTITY.write(buffer, entity));
    }

    public interface IEntityList {
        Collection<EntityType<?>> getEntities();

        JsonObject serialize();
    }

    public static class SingleEntityList implements IEntityList {
        private final EntityType<?> entity;

        public SingleEntityList(EntityType<?> entity) {
            this.entity = entity;
        }

        public Collection<EntityType<?>> getEntities() {
            return Collections.singleton(this.entity);
        }

        // since forge registries have no guaranteed non-null default element, I'll use the vanilla one for now...
        @SuppressWarnings("deprecation")
        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("entity", Registry.ENTITY_TYPE.getKey(this.entity).toString());
            return jsonobject;
        }
    }

    public static class TagList implements IEntityList {
        private final Tag<EntityType<?>> tag;

        public TagList(Tag<EntityType<?>> tagIn) {
            this.tag = tagIn;
        }

        public Collection<EntityType<?>> getEntities() {
            return this.tag.getValues();
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            // func_232975_b_ = checkId
            jsonobject.addProperty("tag", EntityTypeTags.getAllTags().getIdOrThrow(tag).toString());
            return jsonobject;
        }
    }

    @Override
    protected void updateEmpty()
    {
        
    }

    @Override
    public boolean roll()
    {
        return false;
    }
}