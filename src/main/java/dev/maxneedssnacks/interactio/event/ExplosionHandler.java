package dev.maxneedssnacks.interactio.event;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.CraftingInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import lombok.Value;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.stream.Collectors;

public class ExplosionHandler {

    @SubscribeEvent
    public static void boom(ExplosionEvent.Detonate event) {
        if (event.getWorld().isRemote) return;

        List<ItemEntity> entities = event.getAffectedEntities()
                .stream()
                .filter(Utils::isItem)
                .map(ItemEntity.class::cast)
                .filter(e -> InWorldRecipeType.ITEM_EXPLODE.isValidInput(e.getItem()))
                .collect(Collectors.toList());

        List<BlockPos> blocks = event.getAffectedBlocks();

        InWorldRecipeType.ITEM_EXPLODE
                .applyAll(recipe -> recipe.canCraft(entities),
                        recipe -> recipe.craft(entities, new ExplosionInfo(event.getWorld(), event.getExplosion())));

        // since we're removing blocks from the affected block list, we need to do this
        Lists.newArrayList(blocks).forEach(pos -> {
            if (!blocks.contains(pos)) return;
            BlockState state = event.getWorld().getBlockState(pos);
            if (state.getBlock().equals(Blocks.AIR)) return;

            InWorldRecipeType.BLOCK_EXPLODE
                    .apply(recipe -> recipe.canCraft(pos, state),
                            recipe -> recipe.craft(pos, new ExplosionInfo(event.getWorld(), event.getExplosion())));
        });

    }

    @Value
    public static class ExplosionInfo implements CraftingInfo {
        World world;
        Explosion explosion;
    }
}
