package ky.someone.mods.interactio.recipe.base;

import static com.google.common.base.Predicates.not;
import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.runAll;
import static ky.someone.mods.interactio.Utils.shrinkAndUpdate;
import static ky.someone.mods.interactio.Utils.testAll;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraftforge.registries.ForgeRegistryEntry;

/**
 * @param <T> A type of input or inputs. This will be what the recipe uses during crafts.
 * @param <S> Some kind of state that will be used to check whether a craft should be performed.
 * @param <U> A recipe info wrapper that will be used to provide information to {@link #craft(T, U)}
 */
public abstract class InWorldRecipe<T, S extends StateHolder<?, ?>, U extends CraftingInfo> implements Recipe<Container> {

    protected final JsonObject json;
    
    /** Conditions required for the crafting to begin, run once during {@link #canCraft(Object, StateHolder)} */
    protected final List<BiPredicate<T, S>> startCraftConditions;
    /** Conditions required for each individual craft to occur, run before each loop through the actual crafting process
     * in {@link #craft(Object, CraftingInfo)} */
    protected final List<BiPredicate<T, U>> keepCraftingConditions;
    /** Events to run at the start of {@link #craft(Object, CraftingInfo)}, before any crafting has occurred */
    protected final List<BiConsumer<T, U>> onCraftStart;
    /** Events to run each time an actual craft will happen in {@link #craft(Object, CraftingInfo)}. Examples: Repairing a tool, damaging an anvil */
    protected final List<BiConsumer<T, U>> preCraft;
    /** Events to run after each time a craft happens in {@link #craft(Object, CraftingInfo)}. Examples: Repairing a tool, damaging an anvil */
    protected final List<BiConsumer<T, U>> postCraft;
    /** Events to run at the end of {@link #craft(Object, CraftingInfo)}, once all crafting has completed */
    protected final List<BiConsumer<T, U>> onCraftEnd;
    
    protected final ResourceLocation id;
    protected final List<ItemIngredient> itemInputs;
    protected final BlockIngredient blockInput;
    protected final FluidIngredient fluidInput;
    protected final DynamicOutput output;
    
    public InWorldRecipe(ResourceLocation id, @Nullable List<ItemIngredient> itemInputs, @Nullable BlockIngredient blockInput, @Nullable FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, JsonObject json)
    {
        this.id = id;
        this.output = output;
        this.json = json;
        
        this.itemInputs = itemInputs == null ? Collections.emptyList() : itemInputs;
        this.blockInput = blockInput == null ? BlockIngredient.EMPTY : blockInput;
        this.fluidInput = fluidInput == null ? FluidIngredient.EMPTY : fluidInput;
        
        this.startCraftConditions = new LinkedList<>();
        this.keepCraftingConditions = new LinkedList<>();
        this.onCraftStart = new LinkedList<>();
        this.preCraft = new LinkedList<>();
        this.postCraft = new LinkedList<>();
        this.onCraftEnd = new LinkedList<>();
        
        this.keepCraftingConditions.add((t, u) -> canRunParallel);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * In-world recipes do not make use of any actual inventories.
     * This only exists is so we can have {@link net.minecraft.world.item.crafting.RecipeManager}
     * load all of our recipes correctly.
     *
     * @deprecated Use {@link #canCraft(T, S)} for validation instead.
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
    public abstract boolean canCraft(Level world, T inputs, S state);

    /**
     * Attempts to perform an in-world crafting recipe with the given parameters.
     * Beware: This does *not* necessarily check whether the craft can be performed first,
     * so make sure to run {@link #canCraft(T, S)} first if you want to ensure nothing goes wrong.
     *
     * @param inputs Collection (or otherwise) of inputs (for example item entities).
     *               This object WILL be manipulated by this method,
     *               use {@link #canCraft(T, S)} if you don't want that to happen.
     * @param info   Additional information on the craft, like the world the craft is happening in or the affected Block's position
     */
    public abstract void craft(T inputs, U info);
    
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
        return true;
    }

    public ResourceLocation getId() { return this.id; }
    public List<ItemIngredient> getItemInputs() { return this.itemInputs; }
    public BlockIngredient getBlockInput() { return this.blockInput; }
    public FluidIngredient getFluidInput() { return this.fluidInput; }
    public DynamicOutput getOutput() { return this.output; }
    
    public static abstract class InWorldRecipeSerializer<R extends InWorldRecipe<?,?,?>> extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<R>
    {
        @Override
        public R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer)
        {
            return fromJson(id, GsonHelper.parse(new String(buffer.readByteArray(), StandardCharsets.UTF_8)));
        }
        
        @Override
        public void toNetwork(FriendlyByteBuf buffer, R recipe)
        {
            buffer.writeByteArray(recipe.json.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        protected List<ItemIngredient> parseItemIngredients(ResourceLocation id, JsonObject json, String key)
        {
            List<ItemIngredient> inputs = new ArrayList<>();
            GsonHelper.getAsJsonArray(json, "inputs").forEach(input -> {
                ItemIngredient stack = ItemIngredient.deserialize(input);
                if (!stack.getIngredient().isEmpty())
                    inputs.add(stack);
            });
            if (inputs.isEmpty())
                throw new JsonParseException(String.format("No valid inputs specified for recipe %s!", id));
            return inputs;
        }
    }
    
    public static <S extends StateHolder<?,?>, I extends CraftingInfo> void craftItemList(InWorldRecipe<List<ItemEntity>, S, I> recipe, List<ItemEntity> inputs, I info)
    {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();
        
        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();
        
        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(inputs);
        
        runAll(recipe.onCraftStart, loopingEntities, info);
        do {
            runAll(recipe.preCraft, loopingEntities, info);
            shrinkAndUpdate(used);
            recipe.output.spawn(world, pos);
            runAll(recipe.postCraft, loopingEntities, info);
            
            loopingEntities.removeIf(not(ItemEntity::isAlive));
            used.clear();
        }
        while (compareStacks(loopingEntities, used, recipe.itemInputs) && testAll(recipe.keepCraftingConditions, loopingEntities, info));
        runAll(recipe.onCraftEnd, loopingEntities, info);
    }
    
    public static <S extends StateHolder<?,?>, I extends CraftingInfo> void craftBlock(InWorldRecipe<BlockPos, S, I> recipe, BlockPos input, I info)
    {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();
        
        runAll(recipe.preCraft, pos, info);
        world.destroyBlock(pos, false);
        recipe.output.spawn(world, pos);
        runAll(recipe.postCraft, pos, info);
    }
}
