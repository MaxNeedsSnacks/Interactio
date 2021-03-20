package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.sendParticle;

import java.util.List;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.base.StatelessItemRecipe;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.ingredient.ItemIngredient;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.phys.Vec3;

public final class ItemLightningRecipe extends StatelessItemRecipe<DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final double chance;

    public ItemLightningRecipe(ResourceLocation id, List<ItemIngredient> inputs, DynamicOutput output, double chance, JsonObject json) {
        super(id, inputs, null, null, output, true, json);
        this.chance = chance;
        
        this.postCraft.add((entities, info) -> sendParticle(ParticleTypes.END_ROD, info.getWorld(), Vec3.atCenterOf(info.getPos())));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.ITEM_LIGHTNING;
    }

    public double getChance() {
        return this.chance;
    }
    
    @Override public boolean hasInvulnerableOutput() { return true; }

    public static class Serializer extends InWorldRecipeSerializer<ItemLightningRecipe> {
        @Override
        public ItemLightningRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));

            List<ItemIngredient> inputs = this.parseItemIngredients(id, json, "inputs");

            double chance = Utils.parseChance(json, "chance", 1);

            return new ItemLightningRecipe(id, inputs, output, chance, json);
        }
    }
}
