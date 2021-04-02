package ky.someone.mods.interactio.event;

import static ky.someone.mods.interactio.Utils.isAnvil;

import java.util.List;
import java.util.stream.Collectors;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.base.InWorldRecipeType;
import ky.someone.mods.interactio.recipe.duration.DurationManager;
import ky.someone.mods.interactio.recipe.util.DefaultInfo;
import ky.someone.mods.interactio.recipe.util.EntityInfo;
import ky.someone.mods.interactio.recipe.util.ExplosionInfo;
import me.shedaniel.architectury.event.events.BlockEvent;
import me.shedaniel.architectury.event.events.EntityEvent;
import me.shedaniel.architectury.event.events.ExplosionEvent;
import me.shedaniel.architectury.event.events.LightningEvent;
import me.shedaniel.architectury.event.events.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum InteractioEventHandler {

    ;
    
    public static void init() {
        ExplosionEvent.DETONATE.register(InteractioEventHandler::boom);
        LightningEvent.STRIKE.register(InteractioEventHandler::bzzt);
        BlockEvent.FALLING_LAND.register(InteractioEventHandler::acme);
        EntityEvent.LIVING_DEATH.register(InteractioEventHandler::oof);
        TickEvent.SERVER_WORLD_POST.register(DurationManager::tickAllRecipes);
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
                .applyAll(recipe -> recipe.canCraft(items, new ExplosionInfo(recipe, level, explosion)),
                        recipe -> recipe.craft(items, new ExplosionInfo(recipe, level, explosion)));

        // since we're removing blocks from the affected block list, we need to do this
        blocks.stream().filter(pos -> !level.isEmptyBlock(pos)).forEach(pos -> {
            BlockState state = level.getBlockState(pos);

            InWorldRecipeType.BLOCK_EXPLODE
                    .apply(recipe -> recipe.canCraft(pos, state, new ExplosionInfo(recipe, level, explosion)),
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

        InWorldRecipeType.ITEM_LIGHTNING.applyAll(recipe -> recipe.canCraft(entities, new DefaultInfo(recipe, level, bolt.blockPosition())),
                recipe -> recipe.craft(entities, new DefaultInfo(recipe, level, bolt.blockPosition())));

        BlockPos target = bolt.blockPosition().below();
        BlockState state = level.getBlockState(target);
        InWorldRecipeType.BLOCK_LIGHTNING.applyAll(recipe -> recipe.canCraft(target, state, new DefaultInfo(recipe, level, target)),
                recipe -> recipe.craft(target, new DefaultInfo(recipe, level, target)));
        
        bolt.remove();
    }
    
    public static void acme(Level level, BlockPos pos, BlockState fallState, BlockState landOn, FallingBlockEntity entity) {
        if (!isAnvil(fallState)) return;

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        BlockPos hitPos = pos.below();
        BlockState hitState = level.getBlockState(hitPos);

        InWorldRecipeType.ITEM_ANVIL.applyAll(recipe -> recipe.canCraft(items, hitState, new DefaultInfo(recipe, level, pos)),
                recipe -> recipe.craft(items, new DefaultInfo(recipe, level, pos)));

        InWorldRecipeType.BLOCK_ANVIL.apply(recipe -> recipe.canCraft(pos, hitState, new DefaultInfo(recipe, level, hitPos)),
                recipe -> recipe.craft(pos, new DefaultInfo(recipe, level, hitPos)));

    }
    
    public static InteractionResult oof(LivingEntity entity, DamageSource source) {
        actualOof(entity, source);
        return InteractionResult.PASS;
    }
    
    private static void actualOof(LivingEntity entity, DamageSource source) {
        if (!(source instanceof EntityDamageSource)) return;
        if (source.getDirectEntity() == null) return;
        
        Level level = entity.level;
        BlockPos pos = entity.blockPosition();
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
        BlockPos onPos = pos.below();
        BlockState onState = level.getBlockState(onPos);
        
        InWorldRecipeType.ITEM_ENTITY_KILL.applyAll(recipe -> recipe.canCraft(entity, items, new EntityInfo(recipe, level, entity)),
                recipe -> recipe.craft(items, new EntityInfo(recipe, level, entity)));
        
        InWorldRecipeType.BLOCK_ENTITY_KILL.applyAll(recipe -> recipe.canCraft(entity, onPos, onState, new EntityInfo(recipe, level, entity)),
                recipe -> recipe.craft(onPos, new EntityInfo(recipe, level, entity)));
    }
}
