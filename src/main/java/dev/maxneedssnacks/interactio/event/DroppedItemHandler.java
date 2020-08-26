package dev.maxneedssnacks.interactio.event;

import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static dev.maxneedssnacks.interactio.Utils.isItem;

public class DroppedItemHandler {

    private final Collection<ItemEntity> watching = new ObjectOpenHashSet<>();

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

        watching.removeIf(entity -> !entity.isAlive());
        watching.forEach(entity -> {

            if (!event.world.equals(entity.world)) return;

            World world = event.world;

            BlockPos pos = entity.getPosition();
            AxisAlignedBB region = new AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

            List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, region, e ->
                    world.getBlockState(e.getPosition()).equals(world.getBlockState(pos)) && watching.contains(e));

            matchers.forEach(f -> f.accept(items, world, pos));
        });
    }
}
