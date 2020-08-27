package dev.maxneedssnacks.interactio.event;

import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;

import static dev.maxneedssnacks.interactio.Utils.isItem;

public class DroppedItemHandler {

    private final ObjectSet<ItemEntity> watching = new ObjectOpenHashSet<>();

    private final List<TriConsumer<List<ItemEntity>, World, BlockPos>> matchers = new ArrayList<>();

    public DroppedItemHandler() {

        // item fluid transform
        matchers.add((items, world, pos) -> {
            InWorldRecipeType.ITEM_FLUID_TRANSFORM
                    .apply(recipe -> recipe.canCraft(items, world.getFluidState(pos)),
                            recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
        });

        // fluid fluid transform
        matchers.add((items, world, pos) -> {
            InWorldRecipeType.FLUID_FLUID_TRANSFORM
                    .apply(recipe -> recipe.canCraft(items, world.getFluidState(pos)),
                            recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
        });

    }

    @SubscribeEvent
    public void challengerApproaching(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !isItem(event.getEntity())) return;

        ItemEntity entity = (ItemEntity) event.getEntity();
        ItemStack stack = entity.getItem();

        if (InWorldRecipeType.ITEM_FLUID_TRANSFORM.isValidInput(stack)
                || InWorldRecipeType.FLUID_FLUID_TRANSFORM.isValidInput(stack)) {
            watching.add(entity);
        }
    }

    @SubscribeEvent
    public void t1ckt0ck(TickEvent.WorldTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) return;

        World world = event.world;

        for (ObjectIterator<ItemEntity> iterator = watching.iterator(); iterator.hasNext(); ) {
            ItemEntity entity = iterator.next();
            if (!entity.isAlive()) {
                iterator.remove();
            } else {
                if (!event.world.equals(entity.world)) {
                    continue;
                }
                BlockPos pos = entity.getPosition();

                List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class,
                        entity.getBoundingBox().grow(0.5D, 0.0D, 0.5D), watching::contains);

                matchers.forEach(f -> f.accept(items, world, pos));

            }
        }

    }
}
