package dev.maxneedssnacks.interactio.event;

import dev.maxneedssnacks.interactio.recipe.FluidFluidTransformRecipe;
import dev.maxneedssnacks.interactio.recipe.ItemFluidTransformRecipe;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.ArrayList;
import java.util.List;

import static dev.maxneedssnacks.interactio.Utils.getInWorldRecipeStream;
import static dev.maxneedssnacks.interactio.Utils.isItem;

public class DroppedItemHandler {

    private final NonNullList<ItemEntity> watching = NonNullList.create();
    private final NonNullList<ItemEntity> busy = NonNullList.create();

    private final List<TriConsumer<List<ItemEntity>, World, BlockPos>> matchers = new ArrayList<>();

    public DroppedItemHandler() {

        // item fluid transform
        matchers.add((items, world, pos) -> {
            getInWorldRecipeStream(ItemFluidTransformRecipe.class)
                    .ifPresent(stream -> {
                        stream.filter(recipe -> recipe.canCraft(items, world.getFluidState(pos)))
                                .findFirst()
                                .ifPresent(recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
                    });
        });

        // fluid fluid transform
        matchers.add((items, world, pos) -> {
            getInWorldRecipeStream(FluidFluidTransformRecipe.class)
                    .ifPresent(stream -> {
                        stream.filter(recipe -> recipe.canCraft(items, world.getFluidState(pos)))
                                .findFirst()
                                .ifPresent(recipe -> recipe.craft(items, new InWorldRecipe.DefaultInfo(world, pos)));
                    });
        });

    }

    @SubscribeEvent
    public void challengerApproaching(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !isItem(event.getEntity())) return;

        ItemEntity entity = (ItemEntity) event.getEntity();
        ItemStack stack = entity.getItem();

        if (ModRecipes.ANY_VALID.test(stack)) {
            watching.add(entity);
        }
    }

    @SubscribeEvent
    public void t1ckt0ck(TickEvent.WorldTickEvent event) {
        if (event.side.isClient()) return;

        watching.removeIf(entity -> !entity.isAlive() || busy.contains(entity));
        watching.forEach(entity -> {

            if (!event.world.equals(entity.world)) return;

            World world = event.world;

            BlockPos pos = entity.getPosition();
            AxisAlignedBB region = new AxisAlignedBB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

            List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, region, e -> isItem(e) && watching.contains(e));
            items.removeIf(item -> !world.getBlockState(item.getPosition()).equals(world.getBlockState(pos)));

            busy.addAll(items);
            matchers.forEach(f -> f.accept(items, world, pos));
            busy.removeIf(items::contains);
        });
    }
}
