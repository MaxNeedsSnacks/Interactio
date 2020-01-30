package dev.maxneedssnacks.interactio.integration.jei;

import com.google.common.collect.Lists;
import dev.maxneedssnacks.interactio.compat.CompatHoverChecker;
import dev.maxneedssnacks.interactio.compat.CompatUtil;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.List;

public class IconRecipeInfo implements IDrawable {

    private final IDrawable icon;
    private final CompatHoverChecker hoverChecker;
    private final List<String> tooltips;

    public IconRecipeInfo(IGuiHelper guiHelper, String... tooltips) {
        this(guiHelper, Arrays.asList(tooltips));
    }

    public IconRecipeInfo(ItemStack stack, IGuiHelper guiHelper, String... tooltips) {
        this(stack, guiHelper, Arrays.asList(tooltips));
    }

    public IconRecipeInfo(IGuiHelper guiHelper, Iterable<String> tooltips) {
        this(new ItemStack(Items.MOJANG_BANNER_PATTERN), guiHelper, tooltips);
    }

    public IconRecipeInfo(ItemStack stack, IGuiHelper guiHelper, Iterable<String> tooltips) {
        this.icon = guiHelper.createDrawableIngredient(stack);
        this.hoverChecker = new CompatHoverChecker();
        this.tooltips = Lists.newArrayList(tooltips);
    }

    public int getWidth() {
        return icon.getWidth();
    }

    public int getHeight() {
        return icon.getHeight();
    }

    @Override
    public void draw(int xOffset, int yOffset) {
        hoverChecker.updateBounds(yOffset, yOffset + icon.getHeight(), xOffset, xOffset + icon.getWidth());
        icon.draw(xOffset, yOffset);
    }

    public void drawTooltip(int mx, int my) {
        if (hoverChecker.checkHover(mx, my)) {
            Minecraft mc = Minecraft.getInstance();
            MainWindow window = CompatUtil.getMainWindow();
            if (window == null) return;
            CompatUtil.drawHoveringText(tooltips, mx, my, window.getScaledWidth(), window.getScaledHeight(), -1, mc.fontRenderer);
        }
    }

}
