package dev.maxneedssnacks.interactio.recipe.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public interface IEntrySerializer<T> {
    T read(JsonObject json);

    T read(PacketBuffer buffer);

    void write(PacketBuffer buffer, T content);

    IEntrySerializer<ItemStack> ITEM = new IEntrySerializer<ItemStack>() {
        @Override
        public ItemStack read(JsonObject json) {
            return ShapedRecipe.deserializeItem(json);
        }

        @Override
        public ItemStack read(PacketBuffer buffer) {
            return buffer.readItemStack();
        }

        @Override
        public void write(PacketBuffer buffer, ItemStack content) {
            buffer.writeItemStack(content);
        }
    };

    IEntrySerializer<Fluid> FLUID = new IEntrySerializer<Fluid>() {
        @Override
        public Fluid read(JsonObject json) {
            ResourceLocation id = new ResourceLocation(JSONUtils.getString(json, "fluid"));
            return Optional.ofNullable(ForgeRegistries.FLUIDS.getValue(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse fluid with id " + id + "!"));
        }

        @Override
        public Fluid read(PacketBuffer buffer) {
            return buffer.readRegistryIdSafe(Fluid.class);
        }

        @Override
        public void write(PacketBuffer buffer, Fluid content) {
            buffer.writeRegistryId(content);
        }
    };

    IEntrySerializer<Block> BLOCK = new IEntrySerializer<Block>() {
        @Override
        public Block read(JsonObject json) {
            ResourceLocation id = new ResourceLocation(JSONUtils.getString(json, "block"));
            return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse block with id " + id + "!"));

        }

        @Override
        public Block read(PacketBuffer buffer) {
            return buffer.readRegistryIdSafe(Block.class);
        }

        @Override
        public void write(PacketBuffer buffer, Block content) {
            buffer.writeRegistryId(content);
        }
    };
}
