package dev.maxneedssnacks.interactio.compat;

import dev.maxneedssnacks.interactio.Interactio;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagCollection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * This class uses means such as reflection to bridge method calls
 * between the 1.14 and 1.15 version of this mod. It is considered
 * rather volatile and may change every time a mapping updates.
 */
@SuppressWarnings({"unchecked", "JavaReflectionMemberAccess"})
public enum CompatUtil {

    INSTANCE;

    public static final String version = Interactio.PROXY.getVersion();

    @OnlyIn(Dist.CLIENT)
    public static void drawWithAlpha(IDrawable element) {

        //luckily these two class names aren't obfuscated, so we can just use Class.forName() on them
        Class<?> renderer = getClass("com.mojang.blaze3d.platform.GlStateManager")
                .orElseGet(() -> getClass("com.mojang.blaze3d.systems.RenderSystem")
                        .orElseThrow(() -> new RuntimeException("Neither GlStateManager nor RenderSystem are present, are you sure you're on the right version?")));

        try {
            renderer.getMethod("enableAlphaTest").invoke(null);
            renderer.getMethod("enableBlend").invoke(null);
            element.draw();
            renderer.getMethod("disableBlend").invoke(null);
            renderer.getMethod("disableAlphaTest").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Enabling and/or disabling alpha and blending failed, this is bad...");
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static MainWindow getMainWindow() {
        try {
            if (version.startsWith("1.14")) {
                // mainWindow = field_195558_d
                return (MainWindow) ObfuscationReflectionHelper.findField(Minecraft.class, "field_195558_d").get(Minecraft.getInstance());
            } else if (version.startsWith("1.15")) {
                // yarn - getWindow() = func_228018_at_
                return (MainWindow) ObfuscationReflectionHelper.findMethod(Minecraft.class, "func_228018_at_").invoke(Minecraft.getInstance());
            } else {
                throw new RuntimeException("Unsupported version!");
            }
        } catch (Exception e) {
            Interactio.LOGGER.warn("Error while trying to get main window, returning null! (This is bad.)", e);
            return null;
        }
    }

    @Nullable
    public static TagCollection<Fluid> getFluidTags() {
        Interactio.LOGGER.info(version);
        try {
            if (version.startsWith("1.14")) {
                return (TagCollection<Fluid>) FluidTags.class.getMethod("getCollection").invoke(null);
            } else if (version.startsWith("1.15")) {
                return (TagCollection<Fluid>) ObfuscationReflectionHelper.findMethod(FluidTags.class, "func_226157_a_").invoke(null);
            } else {
                throw new RuntimeException("Unsupported version!");
            }
        } catch (Exception e) {
            Interactio.LOGGER.warn("Error while trying to get fluid tag collection, returning null! (This is bad.)", e);
            return null;
        }
    }

    private static Optional<Class<?>> getClass(String name) {
        try {
            return Optional.of(Class.forName(name));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
