package dev.maxneedssnacks.interactio.mixin;

import dev.maxneedssnacks.interactio.Utils;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Mixin(LightningBoltEntity.class)
public abstract class LightningBoltEntityMixin extends Entity {

    private boolean handled;

    public LightningBoltEntityMixin(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
        throw new IllegalStateException();
    }

    @Inject(method = "tick",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/World;getEntitiesInAABBexcluding(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/function/Predicate;)Ljava/util/List;",
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
                recipe -> recipe.craft(entities, new InWorldRecipe.DefaultInfo(this.world, new BlockPos(this))));

        handled = true;
    }

}
