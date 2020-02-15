package dev.maxneedssnacks.interactio.network;

import dev.maxneedssnacks.interactio.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;
import java.util.function.Supplier;

public class PacketCraftingParticle {

    private static final BasicParticleType DEFAULT_PARTICLE = ParticleTypes.END_ROD;
    private static final int DEFAULT_COUNT = 5;

    private final double x, y, z;

    private final ResourceLocation type;
    private final int count;

    public PacketCraftingParticle(double x, double y, double z) {
        this(x, y, z, DEFAULT_PARTICLE.getRegistryName());
    }

    public PacketCraftingParticle(double x, double y, double z, ResourceLocation type) {
        this(x, y, z, type, DEFAULT_COUNT);
    }

    public PacketCraftingParticle(double x, double y, double z, int count) {
        this(x, y, z, DEFAULT_PARTICLE.getRegistryName(), count);
    }

    public PacketCraftingParticle(double x, double y, double z, ResourceLocation type, int count) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.count = count;
    }

    public static void write(PacketCraftingParticle msg, PacketBuffer buffer) {
        buffer.writeDouble(msg.x);
        buffer.writeDouble(msg.y);
        buffer.writeDouble(msg.z);

        boolean non_default = !msg.type.equals(DEFAULT_PARTICLE.getRegistryName());
        buffer.writeBoolean(non_default);
        if (non_default) {
            buffer.writeResourceLocation(msg.type);
        }
        buffer.writeVarInt(msg.count);
    }

    public static PacketCraftingParticle read(PacketBuffer buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();

        ResourceLocation type = buffer.readBoolean() ? buffer.readResourceLocation() : DEFAULT_PARTICLE.getRegistryName();
        int count = buffer.readVarInt();

        return new PacketCraftingParticle(x, y, z, type, count);
    }

    public static class Handler {

        public static void handle(PacketCraftingParticle msg, Supplier<NetworkEvent.Context> contextSupplier) {
            Context ctx = contextSupplier.get();
            Utils.ensureClientSide(ctx);

            ctx.enqueueWork(() -> {
                Minecraft mc = Minecraft.getInstance();

                World world = mc.world;
                if (world == null) return;

                int count = msg.count;

                BasicParticleType particle = (BasicParticleType) ForgeRegistries.PARTICLE_TYPES.getValue(msg.type);

                Random rand = world.rand;

                for (int i = 0; i < count; i++) {
                    double dx = rand.nextGaussian() / 50;
                    double dy = rand.nextGaussian() / 50;
                    double dz = rand.nextGaussian() / 50;
                    world.addParticle(
                            particle,
                            msg.x - dx,
                            msg.y + MathHelper.nextDouble(rand, 0, 1 - dy),
                            msg.z - dz,
                            dx,
                            dy,
                            dz
                    );
                }
            });

            ctx.setPacketHandled(true);
        }
    }

}
