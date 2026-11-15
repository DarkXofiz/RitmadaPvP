// Java 17+, smooth rotation toward a point with sensitivity-GCD quantisation
package dev.ritmada.pvp.combat;

import dev.ritmada.pvp.config.ProfileSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

final class Aim {
    private static final Random RNG = new Random();

    private Aim() {}

    /** Steps rotation toward pt, returns remaining angular error in degrees. */
    static float at(Minecraft mc, LocalPlayer p, Vec3 pt, ProfileSettings s) {
        Vec3 e = p.getEyePosition();
        double dx = pt.x - e.x, dy = pt.y - e.y, dz = pt.z - e.z;
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float dyaw = Mth.wrapDegrees(ty - p.getYRot());
        float dpit = tp - p.getXRot();
        float sp = Math.max(s.aimSpeed, 0.5f);
        dyaw = Mth.clamp(dyaw, -sp, sp) + jit(s);
        dpit = Mth.clamp(dpit, -sp, sp) + jit(s);

        // anticheat: rotation deltas that aren't multiples of the mouse GCD are the classic aim-check tell
        float g = gcd(mc);
        dyaw = Math.round(dyaw / g) * g;
        dpit = Math.round(dpit / g) * g;

        p.setYRot(p.getYRot() + dyaw);
        p.setXRot(Mth.clamp(p.getXRot() + dpit, -90f, 90f));
        return Math.abs(Mth.wrapDegrees(ty - p.getYRot())) + Math.abs(tp - p.getXRot());
    }

    private static float jit(ProfileSettings s) { return (RNG.nextFloat() - 0.5f) * 2f * s.jitter; }

    private static float gcd(Minecraft mc) {
        double f = mc.options.sensitivity().get() * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
