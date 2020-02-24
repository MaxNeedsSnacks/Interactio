package dev.maxneedssnacks.interactio.network;

import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

import java.util.function.Supplier;

// FIXME: Maybe I can make this work without this unnecessary packet class...
public class PacketInvalidateCache {

    private final boolean invalidate;

    public PacketInvalidateCache(boolean invalidate) {
        this.invalidate = invalidate;
    }

    public static void write(PacketInvalidateCache msg, PacketBuffer buffer) {
        buffer.writeBoolean(msg.invalidate);
    }

    public static PacketInvalidateCache read(PacketBuffer buffer) {
        boolean invalidate = buffer.readBoolean();
        return new PacketInvalidateCache(invalidate);
    }

    public static class Handler {

        public static void handle(PacketInvalidateCache msg, Supplier<Context> contextSupplier) {
            Context ctx = contextSupplier.get();

            if (msg.invalidate) ctx.enqueueWork(InWorldRecipeType::clearCache);

            ctx.setPacketHandled(true);
        }
    }

}
