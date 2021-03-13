package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.compareStacks;
import static ky.someone.mods.interactio.Utils.runAll;
import static ky.someone.mods.interactio.Utils.sendParticle;
import static ky.someone.mods.interactio.Utils.shrinkAndUpdate;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.base.StatelessItemRecipe;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ItemExplosionRecipe extends StatelessItemRecipe<ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    public ItemExplosionRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, JsonObject json) {
        super(id, inputs, null, null, output, json);
    }

    @Override
    public boolean canCraft(List<ItemEntity> entities) {
        return testAll(this.startCraftConditions, entities, null)
                && compareStacks(entities, this.itemInputs);
    }

    @Override
    public void craft(List<ItemEntity> entities, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        Vec3 vec = explosion.getPosition();
        Level world = info.getWorld();
        BlockPos pos = info.getPos();

        Object2IntMap<ItemEntity> used = new Object2IntOpenHashMap<>();

        List<ItemEntity> loopingEntities = Lists.newCopyOnWriteArrayList(entities);

        runAll(this.onCraftStart, entities, info);
        while (compareStacks(loopingEntities, used, this.itemInputs) && testAll(this.keepCraftingConditions, entities, info)) {
            shrinkAndUpdate(used);
            runAll(this.preCraft, entities, info);
            this.output.spawn(world, pos);
            runAll(this.postCraft, entities, info);
            sendParticle(ParticleTypes.END_ROD, world, vec);

            loopingEntities.removeIf(e -> !e.isAlive());
            used.clear();
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
        return InWorldRecipeType.ITEM_EXPLODE;
    }

    public static class Serializer extends InWorldRecipeSerializer<ItemExplosionRecipe> {
        @Override
        public ItemExplosionRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            return new ItemExplosionRecipe(id, inputs, output, json);
        }
    }
}
