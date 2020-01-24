package dev.maxneedssnacks.interactio.event;

import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.ItemExplosionRecipe;
import dev.maxneedssnacks.interactio.recipe.ModRecipes;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
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
                            .forEach(recipe -> recipe.craft(entities, new ItemExplosionRecipe.ExplosionInfo(event.getWorld(), event.getExplosion())));
                });
    }

}
