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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An {@code Ingredient}-equivalent for blocks, based heavily on the vanilla implementation.
 */
public class BlockIngredient extends RecipeIngredient<Block> {

    public static final BlockIngredient EMPTY = new BlockIngredient(Stream.empty());

    private final IBlockList[] acceptedBlocks;
    private Collection<Block> matchingBlocks;

    protected BlockIngredient(Stream<? extends IBlockList> blockLists) {
        super(1, 0);
        this.acceptedBlocks = blockLists.toArray(IBlockList[]::new);
    }

    /**
     * Get a list of all {@link Block}s which match this ingredient. Used for JEI support.
     *
     * @return A list of matching blocks
     */
    public Collection<Block> getMatching() {
        this.determineMatchingBlocks();
        return matchingBlocks;
    }

    private void determineMatchingBlocks() {
        if (this.matchingBlocks == null) {
            this.matchingBlocks = Arrays.stream(this.acceptedBlocks)
                    .flatMap(list -> list.getBlocks().stream())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Test for a match.
     *
     * @param block Block to check the ingredient against.
     * @return True if the block matches the ingredient
     */
    public boolean test(@Nullable Block block) {
        if (block == null) {
            return false;
        } else {
            this.determineMatchingBlocks();
            return matchingBlocks.contains(block);
        }
    }

    /**
     * Test for a match using a block. Does not consider block amount.
     *
     * @param state Block state to check the ingredient against
     * @return True if the block matches the ingredient
     */
    public boolean test(@Nullable BlockState state) {
        return test(state == null ? Blocks.AIR : state.getBlock());
    }

    /**
     * Deserialize a {@link BlockIngredient} from JSON.
     *
     * @param json The JSON object
     * @return A new BlockIngredient
     * @throws JsonSyntaxException If the JSON cannot be parsed
     */
    public static BlockIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                return new BlockIngredient(Stream.of(deserializeBlockList(json.getAsJsonObject())));
            } else if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() == 0) {
                    throw new JsonSyntaxException("Array cannot be empty, at least one block or tag must be defined");
                }
                return new BlockIngredient(StreamSupport.stream(arr.spliterator(), false)
                        .map(element -> deserializeBlockList(element.getAsJsonObject())));
            } else {
                throw new JsonSyntaxException("Expected either an object or an array of objects for block ingredient");
            }
        }

        throw new JsonSyntaxException("Block cannot be null");
    }

    public static IBlockList deserializeBlockList(JsonObject json) {
        if (json.has("block") && json.has("tag")) {
            throw new JsonSyntaxException("Block ingredient should have either 'tag' or 'block', not both!");
        } else if (json.has("block")) {
            Block block = IEntrySerializer.BLOCK.read(json);
            return new SingleBlockList(block);
        } else if (json.has("tag")) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            Tag<Block> tag = BlockTags.getAllTags().getTag(id);
            if (tag == null) {
                throw new JsonSyntaxException("Unknown block tag '" + id + "'");
            }
            return new TagList(tag);
        }

        throw new JsonSyntaxException("Block ingredient should have either 'tag' or 'block'");
    }

    /**
     * Reads a {@link BlockIngredient} from a packet buffer. Use with {@link #write(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     * @return A new BlockIngredient
     */
    public static BlockIngredient read(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        return new BlockIngredient(Stream.generate(() -> new SingleBlockList(IEntrySerializer.BLOCK.read(buffer))).limit(size));
    }

    /**
     * Writes the ingredient to a packet buffer. Use with {@link #read(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     */
    public void write(FriendlyByteBuf buffer) {
        this.determineMatchingBlocks();
        buffer.writeVarInt(matchingBlocks.size());
        matchingBlocks.forEach(block -> IEntrySerializer.BLOCK.write(buffer, block));
    }

    public interface IBlockList {
        Collection<Block> getBlocks();

        JsonObject serialize();
    }

    public static class SingleBlockList implements IBlockList {
        private final Block block;

        public SingleBlockList(Block block) {
            this.block = block;
        }

        public Collection<Block> getBlocks() {
            return Collections.singleton(this.block);
        }

        // since forge registries have no guaranteed non-null default element, I'll use the vanilla one for now...
        @SuppressWarnings("deprecation")
        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
            return jsonobject;
        }
    }

    public static class TagList implements IBlockList {
        private final Tag<Block> tag;

        public TagList(Tag<Block> tagIn) {
            this.tag = tagIn;
        }

        public Collection<Block> getBlocks() {
            return this.tag.getValues();
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            // func_232975_b_ = checkId
            jsonobject.addProperty("tag", BlockTags.getAllTags().getIdOrThrow(tag).toString());
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