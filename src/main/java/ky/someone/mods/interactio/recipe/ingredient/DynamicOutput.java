package ky.someone.mods.interactio.recipe.ingredient;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class DynamicOutput {
    private enum OutputType {
        BLOCK,
        ITEM,
        FLUID
    }
    
    private final OutputType type;
    
    public final WeightedOutput<Block> blockOutput;
    public final WeightedOutput<ItemStack> itemOutput;
    public final WeightedOutput<Fluid> fluidOutput;

    @Nullable
    public Block getBlock() {
        return isBlock() ? blockOutput.rollOnce() : null;
    }

    @Nullable
    public Collection<ItemStack> getItems() {
        return isItem() ? itemOutput.roll() : null;
    }
    
    @Nullable
    public Fluid getFluid() {
        return isFluid() ? fluidOutput.rollOnce(): null;
    }

    public boolean isBlock() {
        return type == OutputType.BLOCK;
    }

    public boolean isItem() {
        return type == OutputType.ITEM;
    }
    
    public boolean isFluid() {
        return type == OutputType.FLUID;
    }

    @SuppressWarnings("unchecked")
    private <C> DynamicOutput(@Nullable WeightedOutput<C> output, Class<C> cls)
    {
        if (cls == Block.class)
        {
            this.blockOutput = (WeightedOutput<Block>) output;
            this.fluidOutput = null;
            this.itemOutput = null;
            this.type = OutputType.BLOCK;
        }
        else if (cls == ItemStack.class)
        {
            this.blockOutput = null;
            this.itemOutput = (WeightedOutput<ItemStack>) output;
            this.fluidOutput = null;
            this.type = OutputType.BLOCK;
        }
        else if (cls == Fluid.class)
        {
            this.blockOutput = null;
            this.itemOutput = null;
            this.fluidOutput = (WeightedOutput<Fluid>) output;
            this.type = OutputType.BLOCK;
        }
        else throw new IllegalArgumentException("Output type must be among " + Arrays.toString(OutputType.values()) + ", but " + cls.getSimpleName() + " was provided");
    }
    
    public void spawn(Level world, BlockPos pos, boolean invulnerable) {
        Random rand = world.getRandom();
        if (isBlock()) {
            world.setBlockAndUpdate(pos, this.getBlock().defaultBlockState());
        }
        else if (isFluid()) {
            world.setBlockAndUpdate(pos, this.getFluid().defaultFluidState().createLegacyBlock());
        }
        else if (isItem()) {
            Collection<ItemStack> stacks = this.getItems();
            stacks.forEach(stack -> {
                double x = pos.getX() + Mth.nextDouble(rand, 0.25, 0.75);
                double y = pos.getY() + Mth.nextDouble(rand, 0.5, 1);
                double z = pos.getZ() + Mth.nextDouble(rand, 0.25, 0.75);
                
                double vel = Mth.nextDouble(rand, 0.1, 0.25);
                
                ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                newItem.setDeltaMovement(0, vel, 0);
                if (invulnerable) newItem.setInvulnerable(invulnerable);
                world.addFreshEntity(newItem);
            });
        }
    }

    public static DynamicOutput create(JsonObject json) {
        // 4 cases to check
        if (json.has("block")) {
            // single block
            return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), Block.class);
        } else if (json.has("fluid")) {
            // single fluid
            return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.FLUID), Fluid.class);
        } else if (json.has("item")) {
            // single item
            return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.ITEM), ItemStack.class);
        } else {
            // assume it's a weighted output
            // try to get a type variable, or error otherwise
            if (json.has("type")) {
                switch (GsonHelper.getAsString(json, "type")) {
                    case "item":
                        return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.ITEM), ItemStack.class);
                    case "block":
                        return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), Block.class);
                    case "fluid":
                        return new DynamicOutput(Utils.singleOrWeighted(json, IEntrySerializer.FLUID), Fluid.class);
                    default:
                        throw new JsonSyntaxException("Unsupported type for output on block explosion recipe!");
                }
            } else {
                throw new JsonSyntaxException("Weighted output types are ambiguous -- please add a 'type' attribute to clarify which type of output you want!");
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        if (isItem()) {
            buf.writeByte(0);
            itemOutput.write(buf, IEntrySerializer.ITEM);
        } else if (isBlock()) {
            buf.writeByte(1);
            blockOutput.write(buf, IEntrySerializer.BLOCK);
        } else if (isFluid()) {
            buf.writeByte(2);
            fluidOutput.write(buf, IEntrySerializer.FLUID);
        } else throw new IllegalStateException("Wrong output type!");
    }

    public static DynamicOutput read(FriendlyByteBuf buf) {
        switch (buf.readByte()) {
            // item
            case 0:
                return new DynamicOutput(WeightedOutput.read(buf, IEntrySerializer.ITEM), ItemStack.class);
            // block
            case 1:
                return new DynamicOutput(WeightedOutput.read(buf, IEntrySerializer.BLOCK), Block.class);
            // fluid
            case 2:
                return new DynamicOutput(WeightedOutput.read(buf, IEntrySerializer.FLUID), Fluid.class);
            // error
            default:
                throw new IllegalStateException("Wrong output type id!");
        }
    }
}
