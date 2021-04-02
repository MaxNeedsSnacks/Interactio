package ky.someone.mods.interactio.recipe;

import static ky.someone.mods.interactio.Utils.sendParticle;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import ky.someone.mods.interactio.Utils.TriPredicate;
import ky.someone.mods.interactio.recipe.util.CraftingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
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
        CRAFT_END("onCraftEnd");
        
        public final String jsonName;
        private EventType(String jsonName) { this.jsonName = jsonName; }
    }

    public static Map<ResourceLocation, TriPredicate<Object, StateHolder<?,?>, CraftingInfo>> startPredicates = new HashMap<>();
    public static Map<ResourceLocation, BiPredicate<Object, CraftingInfo>> continuePredicates = new HashMap<>();
    public static Map<ResourceLocation, BiConsumer<Object, CraftingInfo>> events = new HashMap<>();
    
    public static void init()
    {
        events.put(new ResourceLocation("particle"), (arg, info) -> {
            Level world = info.getWorld();
            BlockPos pos = info.getPos();
            Random rand = world.random;
            double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
            double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
            double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);

            sendParticle(ParticleTypes.END_ROD, world, new Vec3(x, y, z));
        });
    }
    
    
}
