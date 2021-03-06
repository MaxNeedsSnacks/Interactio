package ky.someone.mods.interactio.recipe.base;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.Utils.*;
import ky.someone.mods.interactio.recipe.Events;
import ky.someone.mods.interactio.recipe.Events.EventType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

import static ky.someone.mods.interactio.Utils.*;

/**
 * @param <T> A type of input or inputs. This will be what the recipe uses during crafts.
 * @param <S> Some kind of state that will be used to check whether a craft should be performed.
 * @param <U> A recipe info wrapper that will be used to provide information to {@link #craft(T, U)}
 */
public abstract class InWorldRecipe<T, S extends StateHolder<?, ?>, U extends CraftingInfo> implements Recipe<Container> {

    protected final JsonObject json;

    /**
     * Conditions required for the crafting to begin, run once during {@link #canCraft(T, S, U)}
     */
    protected final Map<RecipeStartPredicate<T, S, U>, JsonObject> startCraftConditions;
    /**
     * Conditions required for each individual craft to occur, run before each loop through the actual crafting process
     * in {@link #craft(Object, CraftingInfo)}
     */
    protected final Map<RecipeContinuePredicate<T, U>, JsonObject> keepCraftingConditions;
    /**
     * Events to run at the start of {@link #craft(Object, CraftingInfo)}, before any crafting has occurred
     */
    protected final Map<RecipeEvent<T, U>, JsonObject> onCraftStart;
    /**
     * Events to run each time an actual craft will happen in {@link #craft(Object, CraftingInfo)}. Examples: Repairing a tool, damaging an anvil
     */
    protected final Map<RecipeEvent<T, U>, JsonObject> preCraft;
    /**
     * Events to run after each time a craft happens in {@link #craft(Object, CraftingInfo)}. Examples: Repairing a tool, damaging an anvil
     */
    protected final Map<RecipeEvent<T, U>, JsonObject> postCraft;
    /**
     * Events to run at the end of {@link #craft(Object, CraftingInfo)}, once all crafting has completed
     */
    protected final Map<RecipeEvent<T, U>, JsonObject> onCraftEnd;

    protected final ResourceLocation id;
    protected final List<ItemIngredient> itemInputs;
    protected final BlockIngredient blockInput;
    protected final FluidIngredient fluidInput;
    protected final DynamicOutput output;

    public InWorldRecipe(ResourceLocation id, @Nullable List<ItemIngredient> itemInputs, @Nullable BlockIngredient blockInput, @Nullable FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, JsonObject json) {
        this.id = id;
        this.output = output;
        this.json = json;

        this.itemInputs = itemInputs == null ? Collections.emptyList() : itemInputs;
        this.blockInput = blockInput == null ? BlockIngredient.EMPTY : blockInput;
        this.fluidInput = fluidInput == null ? FluidIngredient.EMPTY : fluidInput;

        this.startCraftConditions = new HashMap<>();
        this.keepCraftingConditions = new HashMap<>();
        this.onCraftStart = new HashMap<>();
        this.preCraft = new HashMap<>();
        this.postCraft = new HashMap<>();
        this.onCraftEnd = new HashMap<>();

        this.keepCraftingConditions.put((t, u, j) -> canRunParallel, null);

        this.parseEvents();
    }

    /**
     * {@inheritDoc}
     * <p>
     * In-world recipes do not make use of any actual inventories.
     * This only exists is so we can have {@link RecipeManager}
     * load all of our recipes correctly.
     *
     * @deprecated Use {@link #canCraft(T, S, U)} for validation instead.
     */
    @Override
    @Deprecated
    public boolean matches(Container inv, Level worldIn) {
        return true;
    }

    /**
     * This is our analogue version to {@link Recipe#matches(Container, Level)}.
     * Use this to determine whether an in-world craft should be performed or not.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities)
     * @param state  State we want to check our inputs against.
     * @return Should this in-world craft be performed?
     */
    public abstract boolean canCraft(T inputs, S state, U info);

    /**
     * Attempts to perform an in-world crafting recipe with the given parameters.
     * Beware: This does *not* necessarily check whether the craft can be performed first,
     * so make sure to run {@link #canCraft(T, S, U)} first if you want to ensure nothing goes wrong.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities).
     *               This object WILL be manipulated by this method,
     *               use {@link #canCraft(T, S, U)} if you don't want that to happen.
     * @param info   Additional information on the craft, like the world the craft is happening in or the affected Block's position
     */
    public abstract void craft(T inputs, U info);

    public abstract boolean hasInvulnerableOutput();

    /**
     * {@inheritDoc}
     *
     * @deprecated see {@link #getResultItem()}
     */
    @Override
    @Deprecated
    public ItemStack assemble(@Nullable Container inv) {
        return getResultItem();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated In-world recipe outputs aren't necessarily single item stacks. Therefore, this method is unreliable and should be avoided.
     */
    @Override
    public ItemStack getResultItem() {
        return ItemStack.EMPTY;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As we don't use any inventory, no recipes would be able to "fit" anyways.
     */
    @Override
    @Deprecated
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public List<ItemIngredient> getItemInputs() {
        return this.itemInputs;
    }

    public BlockIngredient getBlockInput() {
        return this.blockInput;
    }

    public FluidIngredient getFluidInput() {
        return this.fluidInput;
    }

    public DynamicOutput getOutput() {
        return this.output;
    }

    public JsonObject getJson() {
        return this.json;
    }

    public static abstract class InWorldRecipeSerializer<R extends InWorldRecipe<?, ?, ?>> extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<R> {
        @Override
        public R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return fromJson(id, GsonHelper.parse(new String(buffer.readByteArray(), StandardCharsets.UTF_8)));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, R recipe) {
            buffer.writeByteArray(recipe.json.toString().getBytes(StandardCharsets.UTF_8));
        }

        protected List<ItemIngredient> parseItemIngredients(ResourceLocation id, JsonObject json, String key) {
            List<ItemIngredient> inputs = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, key).forEach(input -> {
                ItemIngredient stack = ItemIngredient.deserialize(input);
                if (!stack.getIngredient().isEmpty())
                    inputs.add(stack);
            });
            if (inputs.isEmpty())
                throw new JsonParseException(String.format("No valid inputs specified for recipe %s!", id));
            return inputs;
        }
    }

    @SuppressWarnings("unchecked")
    private void parseEvents() {
        for (EventType eventType : EventType.normalEvents()) {
            if (!json.has(eventType.jsonName)) continue;
            JsonArray array = GsonHelper.getAsJsonArray(json, eventType.jsonName);
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                if (!object.has("type")) continue;
                ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(object, "type"));

                switch (eventType) {
                    case START_PREDICATES:
                        RecipeStartPredicate<T, S, U> startPredicate = (RecipeStartPredicate<T, S, U>) Events.startPredicates.get(type);
                        if (startPredicate != null) this.startCraftConditions.put(startPredicate, object);
                        break;
                    case CONTINUE_PREDICATES:
                        RecipeContinuePredicate<T, U> continuePredicate = (RecipeContinuePredicate<T, U>) Events.continuePredicates.get(type);
                        if (continuePredicate != null) this.keepCraftingConditions.put(continuePredicate, object);
                        break;
                    case CRAFT_START:
                        RecipeEvent<T, U> event = (RecipeEvent<T, U>) Events.events.get(type);
                        if (event != null) this.onCraftStart.put(event, object);
                        break;
                    case PRE_CRAFT:
                        event = (RecipeEvent<T, U>) Events.events.get(type);
                        if (event != null) this.preCraft.put(event, object);
                        break;
                    case POST_CRAFT:
                        event = (RecipeEvent<T, U>) Events.events.get(type);
                        if (event != null) this.postCraft.put(event, object);
                        break;
                    case CRAFT_END:
                        event = (RecipeEvent<T, U>) Events.events.get(type);
                        if (event != null) this.onCraftEnd.put(event, object);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static <S extends StateHolder<?, ?>, I extends CraftingInfo> void craftItemList(InWorldRecipe<List<ItemEntity>, S, I> recipe, List<ItemEntity> inputs, I info) {
        Level world = info.getWorld();
        BlockPos pos = info.getBlockPos();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(inputs);

        runAll(recipe.onCraftStart, loopingEntities, info);
        compareStacks(loopingEntities, used, recipe.itemInputs);
        do {
            runAll(recipe.preCraft, loopingEntities, info);
            shrinkAndUpdate(used);
            recipe.output.spawn(world, pos, recipe.hasInvulnerableOutput());
            runAll(recipe.postCraft, loopingEntities, info);

            loopingEntities.removeIf(((Predicate<ItemEntity>) ItemEntity::isAlive).negate());
            used.clear();
        }
        while (compareStacks(loopingEntities, used, recipe.itemInputs) && testAll(recipe.keepCraftingConditions, loopingEntities, info));
        runAll(recipe.onCraftEnd, loopingEntities, info);
    }

    public static <S extends StateHolder<?, ?>, I extends CraftingInfo> void craftBlock(InWorldRecipe<BlockPos, S, I> recipe, BlockPos input, I info) {
        Level world = info.getWorld();
        BlockPos pos = info.getBlockPos();

        runAll(recipe.preCraft, pos, info);
        world.destroyBlock(pos, false);
        recipe.output.spawn(world, pos, recipe.hasInvulnerableOutput());
        runAll(recipe.postCraft, pos, info);
    }
}
