package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.runAll;
import static ky.someone.mods.interactio.Utils.sendParticle;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ItemAnvilSmashingRecipe extends InWorldRecipe<List<ItemEntity>, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final double damage;

    public ItemAnvilSmashingRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, double damage, JsonObject json) {
        super(id, inputs, null, null, output, json);
        this.damage = damage;
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities, BlockState state) {
        return testAll(this.startCraftConditions, entities, state)
                && compareStacks(entities, this.itemInputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, DefaultInfo info) {
        Level world = info.getWorld();
        BlockPos pos = info.getPos();
        Random rand = world.getRandom();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        runAll(this.onCraftStart, entities, info);
        while (compareStacks(loopingEntities, used, this.itemInputs) && testAll(this.keepCraftingConditions, entities, info)) {
            // shrink and update items
            Utils.shrinkAndUpdate(used);

            // damage anvil
            boolean anvilBroke = false;
            if (rand.nextDouble() < damage) {
                sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, world.getBlockState(pos)), world, Vec3.atBottomCenterOf(pos), 25);
                BlockState dmg = AnvilBlock.damage(world.getBlockState(pos));
                if (dmg == null) {
                    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    world.levelEvent(1029, pos, 0);
                    anvilBroke = true;
                } else {
                    world.setBlockAndUpdate(pos, dmg);
                }
            }

            runAll(this.preCraft, entities, info);
            output.spawn(world, pos);
            runAll(this.postCraft, entities, info);

            sendParticle(ParticleTypes.END_ROD, world, Vec3.atBottomCenterOf(pos));

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
            
            if (anvilBroke) break;
        }
        runAll(this.onCraftEnd, entities, info);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.itemInputs.stream().map(ItemIngredient::getIngredient).toArray(Ingredient[]::new));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.ITEM_ANVIL_SMASHING;
    }

    public static class Serializer extends InWorldRecipeSerializer<ItemAnvilSmashingRecipe> {
        @Override
        public ItemAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            double damage = Utils.parseChance(json, "damage");

            return new ItemAnvilSmashingRecipe(id, inputs, output, damage, json);
        }
    }
}
