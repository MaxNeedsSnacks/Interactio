package dev.maxneedssnacks.interactio.event;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.BlockExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ItemExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import dev.maxneedssnacks.interactio.recipe.util.CraftingInfo;
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
                .filter(e -> ModRecipes.ANY_VALID.test(e.getItem()))
                .collect(Collectors.toList());

        List<BlockPos> blocks = event.getAffectedBlocks();

        Utils.getInWorldRecipeStream(ItemExplosionRecipe.class)
                .ifPresent(stream -> {
                    stream.filter(recipe -> recipe.canCraft(entities))
                            .forEach(recipe -> recipe.craft(entities, new ExplosionInfo(event.getWorld(), event.getExplosion())));
                });

        // since we're removing blocks from the affected block list, we need to do this
        Lists.newArrayList(blocks).forEach(pos -> {
            if (!blocks.contains(pos)) return;
            BlockState state = event.getWorld().getBlockState(pos);
            if (state.getBlock().equals(Blocks.AIR)) return;
            Utils.getInWorldRecipeStream(BlockExplosionRecipe.class)
                    .flatMap(recipes -> recipes.filter(recipe -> recipe.canCraft(pos, state)).findFirst())
                    .ifPresent(recipe -> recipe.craft(pos, new ExplosionInfo(event.getWorld(), event.getExplosion())));
        });

    }

    @Value
    public static class ExplosionInfo implements CraftingInfo {
        World world;
        Explosion explosion;
    }
}
