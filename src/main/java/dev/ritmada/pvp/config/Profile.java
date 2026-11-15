// Java 17+, profile enum with default tuning
package dev.ritmada.pvp.config;

public enum Profile {
    NOOB("Noob"), PRO("Pro"), HACKER("Hacker");

    public final String label;

    Profile(String label) { this.label = label; }

    //                         aimSpeed jitter miss  reaction minCd reach
    public ProfileSettings defaults() {
        return switch (this) {
            case NOOB   -> new ProfileSettings(5f,  3.0f, 0.25f, 6, 0.60f, 3.0f);
            case PRO    -> new ProfileSettings(20f, 0.8f, 0.05f, 2, 0.90f, 3.0f);
            case HACKER -> new ProfileSettings(90f, 0.0f, 0.00f, 0, 1.00f, 3.0f);
        };
    }
}
