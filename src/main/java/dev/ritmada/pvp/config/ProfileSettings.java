// Java 17+, per-profile tunables (all float so Gson + sliders stay trivial)
package dev.ritmada.pvp.config;

public final class ProfileSettings {
    public float aimSpeed;      // max degrees per tick
    public float jitter;        // random degrees added per tick
    public float missChance;    // chance to skip a ready swing
    public float reactionTicks; // delay after a target shows up
    public float minCooldown;   // attack strength needed before swinging

    public ProfileSettings() {}

    public ProfileSettings(float aimSpeed, float jitter, float missChance,
                           float reactionTicks, float minCooldown) {
        this.aimSpeed = aimSpeed; this.jitter = jitter; this.missChance = missChance;
        this.reactionTicks = reactionTicks; this.minCooldown = minCooldown;
    }
}
