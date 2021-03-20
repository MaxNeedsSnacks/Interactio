package ky.someone.mods.interactio.recipe.ingredient;

import static ky.someone.mods.interactio.Utils.getDouble;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
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
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/**
 * An {@code Ingredient}-equivalent for fluids based partially on SilentChaos512's implementation,
 * as well as partially on Vanilla's {@link net.minecraft.world.item.crafting.Ingredient} class.
 * Does not need support for fluid amount (at least not as of now), so I have omitted that.
 */
public class FluidIngredient implements Predicate<FluidStack> {

    private static final Predicate<? super IFluidList> NON_EMPTY = (list) -> !list.getStacks().stream().allMatch(FluidStack::isEmpty);
    public static final FluidIngredient EMPTY = new FluidIngredient(Stream.empty());

    private final IFluidList[] acceptedFluids;
    private Collection<FluidStack> matchingStacks;
    private int count;

    protected FluidIngredient(Stream<? extends IFluidList> fluidLists) { this(fluidLists, 1); }
    
    protected FluidIngredient(Stream<? extends IFluidList> fluidLists, int count) {
        this.acceptedFluids = fluidLists.filter(NON_EMPTY).toArray(IFluidList[]::new);
        this.count = count;
    }
    

    /**
     * Get a list of all {@link FluidStack}s which match this ingredient. Used for JEI support.
     *
     * @return A list of matching fluids
     */
    public Collection<FluidStack> getMatchingStacks() {
        this.determineMatchingStacks();
        return matchingStacks;
    }

    private void determineMatchingStacks() {
        if (this.matchingStacks == null) {
            this.matchingStacks = Arrays.stream(this.acceptedFluids)
                    .flatMap(list -> list.getStacks().stream())
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Test for a match. Does not consider fluid amount.
     *
     * @param stack Fluid stack to check the ingredient against.
     * @return True if the fluid matches the ingredient
     */
    public boolean test(@Nullable FluidStack stack) {
        if (stack == null) {
            return false;
        } else if (this.acceptedFluids.length == 0) {
            return stack.isEmpty();
        } else {
            this.determineMatchingStacks();
            if (this.matchingStacks.contains(stack))
                return stack.getAmount() >= count;
            return false;
        }
    }

    /**
     * Test for a match using a fluid. Does not consider fluid amount.
     *
     * @param fluid Fluid to check the ingredient against
     * @return True if the fluid matches the ingredient
     */
    public boolean test(Level level, @Nullable Fluid fluid) {
        return test(fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, 1));
    }

    /**
     * Deserialize a {@link FluidIngredient} from JSON.
     *
     * @param json The JSON object
     * @return A new FluidIngredient
     * @throws JsonSyntaxException If the JSON cannot be parsed
     */
    public static FluidIngredient deserialize(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                int count = (int) getDouble(obj, "count", 1);
                if (obj.has("fluids")) {
                    FluidIngredient temp = deserialize(obj.get("fluids"));
                    return new FluidIngredient(Arrays.stream(temp.acceptedFluids), count);
                }
                return new FluidIngredient(Stream.of(deserializeFluidList(json.getAsJsonObject())), count);
            } else if (json.isJsonArray()) {
                JsonArray arr = json.getAsJsonArray();
                if (arr.size() == 0) {
                    throw new JsonSyntaxException("Array cannot be empty, at least one fluid or tag must be defined");
                }
                return new FluidIngredient(StreamSupport.stream(arr.spliterator(), false)
                        .map(element -> deserializeFluidList(element.getAsJsonObject())));
            } else {
                throw new JsonSyntaxException("Expected either an object or an array of objects for fluid ingredient");
            }
        }

        throw new JsonSyntaxException("Fluid cannot be null");
    }

    public static IFluidList deserializeFluidList(JsonObject json) {
        if (json.has("fluid") && json.has("tag")) {
            throw new JsonSyntaxException("Fluid ingredient should have either 'tag' or 'fluid', not both!");
        } else if (json.has("fluid")) {
            Fluid fluid = IEntrySerializer.FLUID.read(json);
            return new SingleFluidList(fluid);
        } else if (json.has("tag")) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "tag"));
            Tag<Fluid> tag = SerializationTags.getInstance().getFluids().getTag(id);
            if (tag == null) {
                throw new JsonSyntaxException("Unknown fluid tag '" + id + "'");
            }
            return new TagList(tag);
        }

        throw new JsonSyntaxException("Fluid ingredient should have either 'tag' or 'fluid'");
    }

    /**
     * Reads a {@link FluidIngredient} from a packet buffer. Use with {@link #write(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     * @return A new FluidIngredient
     */
    public static FluidIngredient read(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Stream<? extends IFluidList> fluids = Stream.generate(() -> new SingleFluidList(FluidStack.readFromPacket(buffer))).limit(size);
        int count = buffer.readVarInt();
        return new FluidIngredient(fluids, count);
    }

    /**
     * Writes the ingredient to a packet buffer. Use with {@link #read(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     */
    public void write(FriendlyByteBuf buffer) {
        this.determineMatchingStacks();
        buffer.writeVarInt(matchingStacks.size());
        matchingStacks.forEach(fluidStack -> new FluidStack(fluidStack, 1).writeToPacket(buffer));
        buffer.writeVarInt(count);
    }

    public interface IFluidList {
        Collection<FluidStack> getStacks();

        JsonObject serialize();
    }

    public static class SingleFluidList implements IFluidList {
        private final FluidStack stack;

        public SingleFluidList(Fluid fluid) {
            this(new FluidStack(fluid, 1000));
        }

        public SingleFluidList(FluidStack stackIn) {
            this.stack = stackIn;
        }

        public Collection<FluidStack> getStacks() {
            return Collections.singleton(this.stack);
        }

        // since forge registries have no guaranteed non-null default element, I'll use the vanilla one for now...
        @SuppressWarnings("deprecation")
        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("fluid", Registry.FLUID.getKey(this.stack.getFluid()).toString());
            return jsonobject;
        }
    }

    public static class TagList implements IFluidList {
        private final Tag<Fluid> tag;

        public TagList(Tag<Fluid> tagIn) {
            this.tag = tagIn;
        }

        public Collection<FluidStack> getStacks() {
            return this.tag.getValues().parallelStream().map(fluid -> new FluidStack(fluid, 1000)).collect(Collectors.toList());
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            // func_232975_b_ = checkId
            jsonobject.addProperty("tag", SerializationTags.getInstance().getFluids().getIdOrThrow(tag).toString());
            return jsonobject;
        }
    }

}