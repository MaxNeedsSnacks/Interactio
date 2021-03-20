package ky.someone.mods.interactio.event;

import java.util.List;
import java.util.stream.Collectors;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.duration.RecipeManager;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import me.shedaniel.architectury.event.events.BlockEvent;
import me.shedaniel.architectury.event.events.ExplosionEvent;
import me.shedaniel.architectury.event.events.LightningEvent;
import me.shedaniel.architectury.event.events.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum InteractioEventHandler {

    ;

    public static void init() {
        ExplosionEvent.DETONATE.register(InteractioEventHandler::boom);
        LightningEvent.STRIKE.register(InteractioEventHandler::bzzt);
        BlockEvent.FALLING_LAND.register(InteractioEventHandler::acme);
        TickEvent.SERVER_WORLD_POST.register(RecipeManager::tickAllRecipes);
    }

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
                .applyAll(recipe -> recipe.canCraft(level, items),
                        recipe -> recipe.craft(items, new ExplosionInfo(recipe, level, explosion)));

        // since we're removing blocks from the affected block list, we need to do this
        blocks.stream().filter(pos -> !level.isEmptyBlock(pos)).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            InWorldRecipeType.BLOCK_EXPLODE
                    .apply(recipe -> recipe.canCraft(level, pos, state),
                            recipe -> recipe.craft(pos, new ExplosionInfo(recipe, level, explosion)));
        });
    }

    public static void bzzt(LightningBolt bolt, Level level, Vec3 pos, List<Entity> toStrike) {
        if (!bolt.isAlive()) return;

        List<ItemEntity> entities = toStrike.stream()
                .filter(Utils::isItem)
                .map(ItemEntity.class::cast)
                .filter(entity -> InWorldRecipeType.ITEM_LIGHTNING.isValidInput(entity.getItem()))
                .collect(Collectors.toList());

        InWorldRecipeType.ITEM_LIGHTNING.applyAll(recipe -> recipe.canCraft(level, entities),
                recipe -> recipe.craft(entities, new DefaultInfo(recipe, level, bolt.blockPosition())));

        bolt.remove();
    }

    public static void acme(Level level, BlockPos pos, BlockState fallState, BlockState landOn, FallingBlockEntity entity) {
        if (fallState.getBlock() != Blocks.ANVIL) return;

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos.offset(1, 1, 1)));
        BlockPos hitPos = pos.below();
        BlockState hitState = level.getBlockState(hitPos);

        InWorldRecipeType.ITEM_ANVIL_SMASHING.applyAll(recipe -> recipe.canCraft(level, items, hitState),
                recipe -> recipe.craft(items, new DefaultInfo(recipe, level, pos)));

        InWorldRecipeType.BLOCK_ANVIL_SMASHING.apply(recipe -> recipe.canCraft(level, pos, hitState),
                recipe -> recipe.craft(pos, new DefaultInfo(recipe, level, hitPos)));

    }
}
