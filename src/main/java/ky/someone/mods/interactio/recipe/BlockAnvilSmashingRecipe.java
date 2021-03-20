package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.sendParticle;
import static ky.someone.mods.interactio.Utils.testAll;

import java.util.Random;

import com.google.gson.JsonObject;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.InWorldRecipe;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.ingredient.BlockIngredient;
import ky.someone.mods.interactio.recipe.ingredient.DynamicOutput;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BlockAnvilSmashingRecipe extends InWorldRecipe<BlockPos, BlockState, DefaultInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    protected final double damage;

    public BlockAnvilSmashingRecipe(ResourceLocation id, BlockIngredient blockInput, DynamicOutput output, double damage, JsonObject json) {
        super(id, null, blockInput, null, output, false, json);
        this.damage = damage;
        
        // after the craft, damage the anvil
        this.postCraft.add((anvilPos, info) -> {
            Level world = info.getWorld();
            Random rand = world.getRandom();
            
            if (rand.nextDouble() < damage) {
                BlockState anvilState = world.getBlockState(anvilPos);
                sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, anvilState), world, Vec3.atBottomCenterOf(anvilPos), 25);
                BlockState dmg = AnvilBlock.damage(anvilState);
                if (dmg == null) {
                    world.setBlockAndUpdate(anvilPos, Blocks.AIR.defaultBlockState());
                    world.levelEvent(1029, anvilPos, 0);
                }
                else world.setBlockAndUpdate(anvilPos, dmg);
            }
        });
    }

    @Override
    public boolean canCraft(Level world, BlockPos pos, BlockState state) {
        return this.blockInput.test(state.getBlock()) && testAll(this.startCraftConditions, pos, state);
    }

    // anvilPos will be the position of the anvil
    // hitPos will be the position of the block hit
    @Override
    public void craft(BlockPos anvilPos, DefaultInfo info) { craftBlock(this, anvilPos, info); }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_ANVIL_SMASHING;
    }
    
    @Override public boolean hasInvulnerableOutput() { return false; }

    private static class Serializer extends InWorldRecipeSerializer<BlockAnvilSmashingRecipe> {
        @Override
        public BlockAnvilSmashingRecipe fromJson(ResourceLocation id, JsonObject json) {
            DynamicOutput output = DynamicOutput.create(GsonHelper.getAsJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(GsonHelper.getAsJsonObject(json, "input"));

            double damage = Utils.parseChance(json, "damage");

            return new BlockAnvilSmashingRecipe(id, input, output, damage, json);
        }
    }
}
