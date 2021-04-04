package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.isAnvil;
import static ky.someone.mods.interactio.Utils.parseChance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.Utils.RecipeContinuePredicate;
import ky.someone.mods.interactio.Utils.RecipeEvent;
import ky.someone.mods.interactio.Utils.RecipeStartPredicate;
import ky.someone.mods.interactio.Utils.RecipeTickEvent;
import ky.someone.mods.interactio.recipe.ingredient.FluidIngredient;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.phys.Vec3;

public class Events
{
    public enum EventType {
        START_PREDICATES("startPredicates"),
        CONTINUE_PREDICATES("continuePredicates"),
        CRAFT_START("onCraftStart"),
        PRE_CRAFT("preCraft"),
        POST_CRAFT("postCraft"),
        CRAFT_END("onCraftEnd"),
        
        TICK("onTick"); // Duration recipe-specific
        
        public final String jsonName;
        private EventType(String jsonName) { this.jsonName = jsonName; }
        
        public static EventType[] normalEvents() {
            return new EventType[] {START_PREDICATES, CONTINUE_PREDICATES, CRAFT_START, PRE_CRAFT, POST_CRAFT, CRAFT_END};
        }
        
        public static EventType[] events() {
            return new EventType[] {CRAFT_START, PRE_CRAFT, POST_CRAFT, CRAFT_END};
        }
    }

    public static Map<ResourceLocation, RecipeStartPredicate<Object, StateHolder<?,?>, CraftingInfo>> startPredicates = new HashMap<>();
    public static Map<ResourceLocation, RecipeContinuePredicate<Object, CraftingInfo>> continuePredicates = new HashMap<>();
    public static Map<ResourceLocation, RecipeEvent<Object, CraftingInfo>> events = new HashMap<>();
    public static Map<ResourceLocation, RecipeTickEvent<Object, StateHolder<?,?>>> tickEvents = new HashMap<>();
    
    public static void init()
    {
        events.put(new ResourceLocation("particle"), (inputs, info, json) -> sendParticle(info));
        tickEvents.put(new ResourceLocation("particle"), (inputs, state, info, json) -> sendParticle(info));
        
        events.put(new ResourceLocation("consume_fluids"), (inputs, info, json) -> {
            FluidIngredient fluidInput = info.getRecipe().getFluidInput();
            if (fluidInput == null) return;

            double chance = parseChance(json, "chance");
            
            Level level = info.getWorld();
            BlockPos pos = info.getBlockPos();
            List<BlockPos> sources = fluidInput.findConnectedSources(level, pos);
            int numSources = sources.size();
            int consumed = (int) (chance * numSources);
            double remaining = chance * numSources - consumed;
            if (level.random.nextDouble() < remaining) consumed++;
            
            if (info.getRecipe().getOutput().isBlock() || info.getRecipe().getOutput().isFluid()) {
                sources.remove(info.getBlockPos());
                consumed--;
            }
            
            for (; consumed > 0 && sources.size() > 0; consumed--) {
                level.setBlockAndUpdate(sources.remove(level.random.nextInt(sources.size())), Blocks.AIR.defaultBlockState());
            }
        });
        
        events.put(new ResourceLocation("damage_anvil"), (inputs, info, json) -> {
            double chance = parseChance(json, "chance");
            
            Level world = info.getWorld();
            Random rand = world.random;
            BlockPos anvilPos;
            if (inputs instanceof BlockPos)
                anvilPos = ((BlockPos) inputs).above();
            else {
                anvilPos = info.getBlockPos();
            }
            
            if (rand.nextDouble() < chance) {
                BlockState anvilState = world.getBlockState(anvilPos);
                if (!isAnvil(anvilState)) return;
                Utils.sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, anvilState), world, Vec3.atBottomCenterOf(anvilPos), 25);
                BlockState dmg = AnvilBlock.damage(anvilState);
                if (dmg == null) {
                    world.setBlockAndUpdate(anvilPos, Blocks.AIR.defaultBlockState());
                    world.levelEvent(1029, anvilPos, 0);
                }
                else world.setBlockAndUpdate(anvilPos, dmg);
            }
        });
        
        continuePredicates.put(new ResourceLocation("damage_anvil"), (inputs, info, json) -> {
            double chance = parseChance(json, "chance");
            
            Level world = info.getWorld();
            Random rand = world.random;
            BlockPos anvilPos;
            if (inputs instanceof BlockPos)
                anvilPos = ((BlockPos) inputs).above();
            else {
                anvilPos = info.getBlockPos();
            }
            
            if (rand.nextDouble() < chance) {
                BlockState anvilState = world.getBlockState(anvilPos);
                if (!isAnvil(anvilState)) return false;
                Utils.sendParticle(new BlockParticleOption(ParticleTypes.BLOCK, anvilState), world, Vec3.atBottomCenterOf(anvilPos), 25);
                BlockState dmg = AnvilBlock.damage(anvilState);
                if (dmg == null) {
                    world.setBlockAndUpdate(anvilPos, Blocks.AIR.defaultBlockState());
                    world.levelEvent(1029, anvilPos, 0);
                    return false;
                }
                else world.setBlockAndUpdate(anvilPos, dmg);
            }
            return true;
        });
    }
    
    private static void sendParticle(CraftingInfo info) {
        Level world = info.getWorld();
        Vec3 pos = info.getPos();
        Random rand = world.random;
        double x = pos.x + Mth.nextDouble(rand, 0.25, 0.75);
        double y = pos.y + Mth.nextDouble(rand, 0.5, 1);
        double z = pos.z + Mth.nextDouble(rand, 0.25, 0.75);

        Utils.sendParticle(ParticleTypes.END_ROD, world, new Vec3(x, y, z));
    }
}
