// Java 17+, ClickGUI: profile / mode / toggle / sliders. Opens on R.
package dev.ritmada.pvp.gui;

import dev.ritmada.pvp.PvpMod;
import dev.ritmada.pvp.config.Config;
import dev.ritmada.pvp.config.Profile;
import dev.ritmada.pvp.config.ProfileSettings;
import dev.ritmada.pvp.config.PvpMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PvpScreen extends Screen {
    private static final int W = 340, H = 268;
    private static final int ACCENT = 0xFF7C5CFF, BG = 0xF0121218, PANEL = 0xFF1C1C26, HOT = 0xFF2A2A3A;
    private static final int WHITE = 0xFFFFFFFF, DIM = 0xFF9A9AB0;

    private record Btn(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier active, Runnable act) {}

    private static final class Slider {
        final String label; final float min, max; final int decimals;
        final Supplier<Float> get; final Consumer<Float> set; final int x, y, w;

        Slider(String label, float min, float max, int decimals, Supplier<Float> get, Consumer<Float> set,
               int x, int y, int w) {
            this.label = label; this.min = min; this.max = max; this.decimals = decimals;
            this.get = get; this.set = set; this.x = x; this.y = y; this.w = w;
        }

        void at(double mx) {
            float t = Mth.clamp((float) ((mx - x) / w), 0f, 1f);
            float v = min + (max - min) * t;
            set.accept(decimals == 0 ? (float) Math.round(v) : v);
        }

        void draw(GuiGraphics g, Font f) {
            float v = get.get();
            String val = decimals == 0 ? String.valueOf(Math.round(v)) : String.format("%.2f", v);
            g.drawString(f, label, x, y, 0xFFDDDDEE, false);
            g.drawString(f, val, x + w - f.width(val), y, DIM, false);
            int fx = x + (int) (w * (v - min) / (max - min));
            g.fill(x, y + 11, x + w, y + 14, PANEL);
            g.fill(x, y + 11, fx, y + 14, ACCENT);
            g.fill(fx - 2, y + 9, fx + 2, y + 16, WHITE);
        }
    }

    private final List<Btn> btns = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();
    private Slider drag;
    private int px, py;

    public PvpScreen() { super(Component.literal("PvP")); }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        btns.clear();

        int x = px + 12, w = (W - 24 - 8) / 3, i = 0;
        for (Profile pr : Profile.values()) {
            btns.add(new Btn(x + i++ * (w + 4), py + 26, w, 18, () -> pr.label,
                    () -> Config.profile == pr, () -> { Config.profile = pr; rebuild(); Config.save(); }));
        }
        w = (W - 24 - 12) / 4; i = 0;
        for (PvpMode m : PvpMode.values()) {
            btns.add(new Btn(x + i++ * (w + 4), py + 50, w, 18, () -> m.shortName,
                    () -> Config.mode == m, () -> { Config.mode = m; rebuild(); Config.save(); }));
        }
        btns.add(new Btn(x, py + 74, W - 24, 18, () -> "Auto PvP: " + (Config.enabled ? "ON" : "OFF"),
                () -> Config.enabled, () -> { Config.enabled = !Config.enabled; Sounds.toggle(Config.enabled); }));
        rebuild();
    }

    private void rebuild() {
        sliders.clear();
        ProfileSettings s = Config.cur();
        int x = px + 12, w = W - 24, y = py + 114;
        add(x, y, w, "Aim Speed (deg/tick)", 1, 180, 2, () -> s.aimSpeed, v -> s.aimSpeed = v); y += 19;
        add(x, y, w, "Aim Jitter (deg)", 0, 6, 2, () -> s.jitter, v -> s.jitter = v); y += 19;
        add(x, y, w, "Miss Chance", 0, 0.6f, 2, () -> s.missChance, v -> s.missChance = v); y += 19;
        add(x, y, w, "Reaction (ticks)", 0, 12, 0, () -> s.reactionTicks, v -> s.reactionTicks = v); y += 19;
        add(x, y, w, "Min Attack Cooldown", 0.3f, 1f, 2, () -> s.minCooldown, v -> s.minCooldown = v); y += 19;
        add(x, y, w, "Reach (blocks)", 2.5f, 4.5f, 2, () -> s.reach, v -> s.reach = v); y += 19;
        PvpMode m = Config.mode;
        add(x, y, w, m.paramLabel, m.min, m.max, 0, () -> Config.param(m), v -> Config.params.put(m, v));
    }

    private void add(int x, int y, int w, String l, float min, float max, int dec,
                     Supplier<Float> g, Consumer<Float> s) {
        sliders.add(new Slider(l, min, max, dec, g, s, x, y, w));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.fill(px, py, px + W, py + H, BG);
        g.fill(px, py, px + W, py + 2, ACCENT);
        g.drawString(font, "PVP", px + 12, py + 9, WHITE, true);
        String st = (Config.enabled ? "ON" : "OFF") + " | " + Config.profile.label + " | " + Config.mode.label;
        g.drawString(font, st, px + W - 12 - font.width(st), py + 9, Config.enabled ? 0xFF55FF88 : 0xFFFF5566, true);

        for (Btn b : btns) {
            boolean hov = in(mx, my, b.x, b.y, b.w, b.h);
            g.fill(b.x, b.y, b.x + b.w, b.y + b.h, b.active.getAsBoolean() ? ACCENT : hov ? HOT : PANEL);
            g.drawCenteredString(font, b.label.get(), b.x + b.w / 2, b.y + 5, WHITE);
        }
        g.drawString(font, Config.profile.label + " / " + Config.mode.label + " settings", px + 12, py + 100, DIM, false);
        for (Slider s : sliders) s.draw(g, font);
        g.drawString(font, "Rebind: Options > Controls > Key Binds", px + 12, py + H - 14, DIM, false);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Btn b : btns) if (in(mx, my, b.x, b.y, b.w, b.h)) { b.act.run(); return true; }
            for (Slider s : sliders) if (in(mx, my, s.x, s.y, s.w, 17)) { drag = s; s.at(mx); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (drag != null) { drag.at(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (drag != null) { drag = null; Config.save(); }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (PvpMod.open.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void onClose() { Config.save(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
