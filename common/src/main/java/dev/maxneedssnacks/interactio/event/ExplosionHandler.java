package dev.maxneedssnacks.interactio.event;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.CraftingInfo;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.stream.Collectors;

public class ExplosionHandler {

    public static void boom(Level level, Explosion explosion, List<Entity> entities) {
        if (level.isClientSide) return;

        List<ItemEntity> items = entities
                .stream()
                .filter(Utils::isItem)
                .map(ItemEntity.class::cast)
                .filter(e -> InWorldRecipeType.ITEM_EXPLODE.isValidInput(e.getItem()))
                .collect(Collectors.toList());

        List<BlockPos> blocks = explosion.getToBlow();

        InWorldRecipeType.ITEM_EXPLODE
                .applyAll(recipe -> recipe.canCraft(items),
                        recipe -> recipe.craft(items, new ExplosionInfo(level, explosion)));

        // since we're removing blocks from the affected block list, we need to do this
        Lists.newArrayList(blocks).forEach(pos -> {
            if (!blocks.contains(pos)) return;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock().equals(Blocks.AIR)) return;

            InWorldRecipeType.BLOCK_EXPLODE
                    .apply(recipe -> recipe.canCraft(pos, state),
                            recipe -> recipe.craft(pos, new ExplosionInfo(level, explosion)));
        });

    }

    public static final class ExplosionInfo implements CraftingInfo {
        private final Level level;
        private final Explosion explosion;

        public ExplosionInfo(Level level, Explosion explosion) {
            this.level = level;
            this.explosion = explosion;
        }

        public Level getLevel() {
            return this.level;
        }

        public Explosion getExplosion() {
            return this.explosion;
        }
    }
}
