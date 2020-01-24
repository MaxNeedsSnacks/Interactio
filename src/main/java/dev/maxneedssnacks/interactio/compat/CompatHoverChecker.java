package dev.maxneedssnacks.interactio.compat;

import dev.maxneedssnacks.interactio.Interactio;

public class CompatHoverChecker {

    // note: this is a HoverChecker object
    private final Object delegate;
    private final Class<?> clazz;

    public CompatHoverChecker() {
        String version = Interactio.PROXY.getVersion();
        try {
            // version <= 1.15.1
            if (version.startsWith("1.14") || version.matches("1\\.15(\\.1)?")) {
                clazz = Class.forName("net.minecraftforge.fml.client.config.HoverChecker");
            } else if (/* >= 1.15.2 */ version.startsWith("1.15")) {
                clazz = Class.forName("net.minecraftforge.fml.client.gui.HoverChecker");
            } else {
                throw new RuntimeException("Unsupported version!");
            }
            delegate = clazz.getConstructor(int.class, int.class, int.class, int.class, int.class)
                    .newInstance(0, 0, 0, 0, 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise compatible HoverChecker!", e);
        }
    }

    public void updateBounds(int top, int bottom, int left, int right) {
        try {
            clazz.getMethod("updateBounds", int.class, int.class, int.class, int.class)
                    .invoke(delegate, top, bottom, left, right);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke updateBounds on HoverChecker!", e);
        }
    }

    public boolean checkHover(int mx, int my) {
        try {
            return (boolean) clazz.getMethod("checkHover", int.class, int.class)
                    .invoke(delegate, mx, my);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke checkHover on HoverChecker!", e);
        }
    }


}
