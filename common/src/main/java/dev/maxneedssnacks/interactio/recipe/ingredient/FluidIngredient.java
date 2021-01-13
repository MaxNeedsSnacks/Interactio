package dev.maxneedssnacks.interactio.recipe.ingredient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.maxneedssnacks.interactio.Interactio;
import dev.maxneedssnacks.interactio.recipe.util.EntrySerializer;
import me.shedaniel.architectury.fluid.FluidStack;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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


    protected FluidIngredient(Stream<? extends IFluidList> fluidLists) {
        this.acceptedFluids = fluidLists.filter(NON_EMPTY).toArray(IFluidList[]::new);
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
            return matchingStacks.contains(stack);
        }
    }

    /**
     * Test for a match using a fluid. Does not consider fluid amount.
     *
     * @param fluid Fluid to check the ingredient against
     * @return True if the fluid matches the ingredient
     */
    public boolean test(@Nullable Fluid fluid) {
        return test(fluid == null ? FluidStack.empty() : FluidStack.create(fluid, FluidStack.bucketAmount()));
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
                return new FluidIngredient(Stream.of(deserializeFluidList(json.getAsJsonObject())));
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
            Fluid fluid = EntrySerializer.FLUID.fromJson(json);
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
     * Reads a {@link FluidIngredient} from a packet buffer. Use with {@link #toNetwork(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     * @return A new FluidIngredient
     */
    public static FluidIngredient fromNetwork(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        return new FluidIngredient(Stream.generate(() -> new SingleFluidList(FluidStack.read(buffer))).limit(size));
    }

    /**
     * Writes the ingredient to a packet buffer. Use with {@link #fromNetwork(FriendlyByteBuf)}.
     *
     * @param buffer The packet buffer
     */
    public void toNetwork(FriendlyByteBuf buffer) {
        this.determineMatchingStacks();
        buffer.writeVarInt(matchingStacks.size());
        matchingStacks.forEach(fluidStack -> fluidStack.write(buffer));
    }

    public interface IFluidList {
        Collection<FluidStack> getStacks();

        JsonObject serialize();
    }

    public static class SingleFluidList implements IFluidList {
        private final FluidStack stack;

        public SingleFluidList(Fluid fluid) {
            this(FluidStack.create(fluid, FluidStack.bucketAmount()));
        }

        public SingleFluidList(FluidStack stackIn) {
            this.stack = stackIn;
        }

        public Collection<FluidStack> getStacks() {
            return Collections.singleton(this.stack);
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("fluid", Interactio.REGISTRIES.get(Registry.FLUID_REGISTRY).getKey(this.stack.getFluid()).toString());
            return jsonobject;
        }
    }

    public static class TagList implements IFluidList {
        private final Tag<Fluid> tag;

        public TagList(Tag<Fluid> tagIn) {
            this.tag = tagIn;
        }

        public Collection<FluidStack> getStacks() {
            return this.tag.getValues().parallelStream().map(fluid -> FluidStack.create(fluid, FluidStack.bucketAmount())).collect(Collectors.toList());
        }

        public JsonObject serialize() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("tag", SerializationTags.getInstance().getFluids().getIdOrThrow(tag).toString());
            return jsonobject;
        }
    }

}