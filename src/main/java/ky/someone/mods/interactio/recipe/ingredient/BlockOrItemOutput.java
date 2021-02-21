package ky.someone.mods.interactio.recipe.ingredient;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ky.someone.mods.interactio.Utils;
import ky.someone.mods.interactio.recipe.util.IEntrySerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.Collection;

public final class BlockOrItemOutput {
    public final WeightedOutput<Block> blockOutput;
    public final WeightedOutput<ItemStack> itemOutput;

    @Nullable
    public Block getBlock() {
        return isBlock() ? blockOutput.rollOnce() : null;
    }

    @Nullable
    public Collection<ItemStack> getItems() {
        return isItem() ? itemOutput.roll() : null;
    }

    public boolean isBlock() {
        return blockOutput != null;
    }

    public boolean isItem() {
        return itemOutput != null;
    }

    private BlockOrItemOutput(@Nullable WeightedOutput<Block> blockOutput, @Nullable WeightedOutput<ItemStack> itemOutput) {
        if (blockOutput != null && itemOutput != null)
            throw new IllegalArgumentException("Either block OR item should be provided, not both!");
        if (blockOutput == null && itemOutput == null)
            throw new IllegalArgumentException("Either block OR item should be provided!");
        this.blockOutput = blockOutput;
        this.itemOutput = itemOutput;
    }

    public static BlockOrItemOutput create(JsonObject json) {
        // 4 cases to check
        if (json.has("block")) {
            // single block
            return new BlockOrItemOutput(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), null);
        } else if (json.has("item")) {
            // single item
            return new BlockOrItemOutput(null, Utils.singleOrWeighted(json, IEntrySerializer.ITEM));
        } else {
            // assume it's a weighted output
            // try to get a type variable, or error otherwise
            if (json.has("type")) {
                switch (GsonHelper.getAsString(json, "type")) {
                    case "item":
                        return new BlockOrItemOutput(null, Utils.singleOrWeighted(json, IEntrySerializer.ITEM));
                    case "block":
                        return new BlockOrItemOutput(Utils.singleOrWeighted(json, IEntrySerializer.BLOCK), null);
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
        } else throw new IllegalStateException("Wrong output type!");
    }

    public static BlockOrItemOutput read(FriendlyByteBuf buf) {
        switch (buf.readByte()) {
            // item
            case 0:
                return new BlockOrItemOutput(null, WeightedOutput.read(buf, IEntrySerializer.ITEM));
            // block
            case 1:
                return new BlockOrItemOutput(WeightedOutput.read(buf, IEntrySerializer.BLOCK), null);
            // error
            default:
                throw new IllegalStateException("Wrong output type id!");
        }
    }
}
