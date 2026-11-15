// Java 17+, status line: system state + active profile + mode
package dev.ritmada.pvp.gui;

import dev.ritmada.pvp.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class Hud {
    private Hud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        String t = "PvP " + (Config.enabled ? "ON" : "OFF") + " | " + Config.profile.label + " | " + Config.mode.label;
        g.drawString(mc.font, t, 6, 6, Config.enabled ? 0xFF55FF88 : 0xFFFF5566, true);
        if (Config.enabled && !mc.isLocalServer())
            g.drawString(mc.font, "sadece kendi dunyanda calisir", 6, 18, 0xFFFFAA33, true);
    }
}
