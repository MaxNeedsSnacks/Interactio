package ky.someone.mods.interactio.recipe.base;

import static com.google.common.base.Predicates.not;
import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.runAll;
import static ky.someone.mods.interactio.Utils.shrinkAndUpdate;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

public abstract class StatelessItemRecipe<U extends CraftingInfo> extends StatelessRecipe<List<ItemEntity>, U> {

    public StatelessItemRecipe(ResourceLocation id, List<ItemIngredient> itemInputs, BlockIngredient blockInput, FluidIngredient fluidInput, DynamicOutput output, boolean canRunParallel, JsonObject json)
    {
        super(id, itemInputs, blockInput, fluidInput, output, canRunParallel, json);
    }
    
    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }
    
    @Override
    public boolean canCraft(Level world, List<ItemEntity> entities) {
        return testAll(this.startCraftConditions, entities, null)
                && compareStacks(entities, this.itemInputs);
    }
    
    @Override
    public void craft(List<ItemEntity> inputs, U info)
    {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();
        
        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();
        
        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(inputs);
        
        runAll(this.onCraftStart, loopingEntities, info);
        do {
            runAll(this.preCraft, loopingEntities, info);
            shrinkAndUpdate(used);
            this.output.spawn(world, pos);
            runAll(this.postCraft, loopingEntities, info);
            
            loopingEntities.removeIf(not(ItemEntity::isAlive));
            used.clear();
        }
        while (compareStacks(loopingEntities, used, this.itemInputs) && testAll(this.keepCraftingConditions, loopingEntities, info));
        runAll(this.onCraftEnd, loopingEntities, info);
    }
}