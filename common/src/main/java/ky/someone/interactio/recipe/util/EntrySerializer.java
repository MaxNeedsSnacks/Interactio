package ky.someone.interactio.recipe.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import ky.someone.interactio.Interactio;
import ky.someone.interactio.Utils;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public interface EntrySerializer<T> {
    T fromJson(JsonObject json);

    T fromNetwork(FriendlyByteBuf buffer);

    void toNetwork(FriendlyByteBuf buffer, T content);

    EntrySerializer<ItemStack> ITEM = new EntrySerializer<ItemStack>() {
        @Override
        public ItemStack fromJson(JsonObject json) {
            return ShapedRecipe.itemFromJson(json);
        }

        @Override
        public ItemStack fromNetwork(FriendlyByteBuf buffer) {
            return buffer.readItem();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemStack content) {
            buffer.writeItem(content);
        }
    };

    EntrySerializer<Fluid> FLUID = new EntrySerializer<Fluid>() {
        @Override
        public Fluid fromJson(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
            return Optional.ofNullable(Interactio.REGISTRIES.get(Registry.FLUID_REGISTRY).get(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse fluid with id " + id + " from JSON!"));
        }

        @Override
        public Fluid fromNetwork(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            return Optional.ofNullable(Interactio.REGISTRIES.get(Registry.FLUID_REGISTRY).get(id))
                    .orElseThrow(() -> new IllegalArgumentException("Unable to parse fluid with id " + id + " from buffer!"));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, Fluid content) {
            buffer.writeResourceLocation(Utils.fluidId(content));
        }
    };

    EntrySerializer<Block> BLOCK = new EntrySerializer<Block>() {
        @Override
        public Block fromJson(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "block"));
            return Optional.ofNullable(Interactio.REGISTRIES.get(Registry.BLOCK_REGISTRY).get(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse block with id " + id + " from JSON!"));
        }

        @Override
        public Block fromNetwork(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            return Optional.ofNullable(Interactio.REGISTRIES.get(Registry.BLOCK_REGISTRY).get(id))
                    .orElseThrow(() -> new IllegalArgumentException("Unable to parse block with id " + id + " from buffer!"));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, Block content) {
            buffer.writeResourceLocation(Utils.blockId(content));
        }
    };
}
