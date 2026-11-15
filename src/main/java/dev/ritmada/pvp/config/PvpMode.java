// Java 17+, PvP modes, one tunable param each
package dev.ritmada.pvp.config;

public enum PvpMode {
    BEAST("Beast PvP", "Beast", "Crit Only (0/1)", 0, 1, 1),
    POT("Netherite Pot PvP", "Pot", "Heal Below HP", 2, 18, 8),
    ELYTRA("Elytra PvP", "Elytra", "Rocket Every (ticks)", 10, 80, 30),
    CRYSTAL("Crystal PvP", "Crystal", "Action Delay (ticks)", 0, 10, 2);

    public final String label, shortName, paramLabel;
    public final float min, max, def;

    PvpMode(String label, String shortName, String paramLabel, float min, float max, float def) {
        this.label = label; this.shortName = shortName; this.paramLabel = paramLabel;
        this.min = min; this.max = max; this.def = def;
    }
}
