package ky.someone.mods.interactio.recipe.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public interface IEntrySerializer<T> {
    T read(JsonObject json);

    T read(FriendlyByteBuf buffer);

    void write(FriendlyByteBuf buffer, T content);

    IEntrySerializer<ItemStack> ITEM = new IEntrySerializer<ItemStack>() {
        @Override
        public ItemStack read(JsonObject json) {
            return ShapedRecipe.itemFromJson(json);
        }

        @Override
        public ItemStack read(FriendlyByteBuf buffer) {
            return buffer.readItem();
        }

        @Override
        public void write(FriendlyByteBuf buffer, ItemStack content) {
            buffer.writeItem(content);
        }
    };

    IEntrySerializer<Fluid> FLUID = new IEntrySerializer<Fluid>() {
        @Override
        public Fluid read(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
            return Optional.ofNullable(ForgeRegistries.FLUIDS.getValue(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse fluid with id " + id + "!"));
        }

        @Override
        public Fluid read(FriendlyByteBuf buffer) {
            return buffer.readRegistryIdSafe(Fluid.class);
        }

        @Override
        public void write(FriendlyByteBuf buffer, Fluid content) {
            buffer.writeRegistryId(content);
        }
    };

    IEntrySerializer<Block> BLOCK = new IEntrySerializer<Block>() {
        @Override
        public Block read(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "block"));
            return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse block with id " + id + "!"));
        }

        @Override
        public Block read(FriendlyByteBuf buffer) {
            return buffer.readRegistryIdSafe(Block.class);
        }

        @Override
        public void write(FriendlyByteBuf buffer, Block content) {
            buffer.writeRegistryId(content);
        }
    };
    
    @SuppressWarnings("rawtypes")
    IEntrySerializer<EntityType> ENTITY = new IEntrySerializer<EntityType>() {
        @Override
        public EntityType<?> read(JsonObject json) {
            ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "entity"));
            return Optional.ofNullable(ForgeRegistries.ENTITIES.getValue(id))
                    .orElseThrow(() -> new JsonParseException("Unable to parse entity with id " + id + "!"));
        }
        @Override
        public EntityType<?> read(FriendlyByteBuf buffer)
        {
            return buffer.readRegistryIdSafe(EntityType.class);
        }
        @Override
        @SuppressWarnings("unchecked")
        public void write(FriendlyByteBuf buffer, EntityType content)
        {
            buffer.writeRegistryId(content);
        }
    };
}
