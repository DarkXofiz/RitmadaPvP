// Java 17+, Fabric client entrypoint, Mojang mappings, MC 1.20.1-1.21.4
package dev.ritmada.pvp;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ritmada.pvp.combat.AutoPvp;
import dev.ritmada.pvp.config.Config;
import dev.ritmada.pvp.gui.Hud;
import dev.ritmada.pvp.gui.PvpScreen;
import dev.ritmada.pvp.gui.Sounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class PvpMod implements ClientModInitializer {
    public static KeyMapping open;
    public static KeyMapping toggle;

    @Override
    public void onInitializeClient() {
        Config.load();
        // registered keybinds show up in Options > Controls, so rebinding in-game is free
        open = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ritmadapvp.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.ritmadapvp"));
        toggle = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ritmadapvp.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.ritmadapvp"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (open.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new PvpScreen());
            }
            while (toggle.consumeClick()) {
                Config.enabled = !Config.enabled;
                Sounds.toggle(Config.enabled);
            }
            AutoPvp.tick(mc);
        });
        HudRenderCallback.EVENT.register((g, t) -> Hud.render(g));
    }
}
