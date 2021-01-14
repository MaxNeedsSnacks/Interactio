package ky.someone.mods.interactio.core.mixin;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.util.InWorldRecipe;
import ky.someone.mods.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(LightningBolt.class)
public abstract class LightningBoltEntityMixin extends Entity {

    private boolean handled;

    public LightningBoltEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
        throw new IllegalStateException();
    }

    @Inject(method = "tick",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    ordinal = 0,
                    shift = At.Shift.BY,
                    by = 1
            ),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void handleLightning(CallbackInfo ci, double d0, List<Entity> list) {
        if (handled) return;

        List<ItemEntity> entities = list.stream()
                .filter(Utils::isItem)
                .map(ItemEntity.class::cast)
                .filter(entity -> InWorldRecipeType.ITEM_LIGHTNING.isValidInput(entity.getItem()))
                .collect(Collectors.toList());

        InWorldRecipeType.ITEM_LIGHTNING.applyAll(recipe -> recipe.canCraft(entities),
                recipe -> recipe.craft(entities, new InWorldRecipe.DefaultInfo(this.level, this.getOnPos())));

        handled = true;
    }

}
