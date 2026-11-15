// Java 17+, eased rotation toward a point: dead zone + proportional step + slow aim-point wobble (no per-tick shake)
package dev.ritmada.pvp.combat;

import dev.ritmada.pvp.config.ProfileSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

final class Aim {
    private static final Random RNG = new Random();
    private static Vec3 off = Vec3.ZERO;
    private static int lastWobble;

    private Aim() {}

    /** Steps rotation toward pt, returns remaining angular error in degrees. */
    static float at(Minecraft mc, LocalPlayer p, Vec3 pt, ProfileSettings s) {
        // wobble: the aim point drifts every ~12 ticks instead of the camera twitching every tick
        if (s.jitter > 0f && p.tickCount - lastWobble > 12) {
            double j = s.jitter * 0.08;
            off = new Vec3((RNG.nextDouble() - .5) * 2 * j, (RNG.nextDouble() - .5) * 2 * j, (RNG.nextDouble() - .5) * 2 * j);
            lastWobble = p.tickCount;
        } else if (s.jitter <= 0f) off = Vec3.ZERO;
        pt = pt.add(off);

        Vec3 e = p.getEyePosition();
        double dx = pt.x - e.x, dy = pt.y - e.y, dz = pt.z - e.z;
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float eyaw = Mth.wrapDegrees(ty - p.getYRot());
        float epit = tp - p.getXRot();
        float err = Math.abs(eyaw) + Math.abs(epit);
        if (err < 1.5f) return err; // dead zone: no micro-corrections, no shake

        float sp = Math.max(s.aimSpeed, 0.5f);
        float k = Mth.clamp(sp / 60f, 0.08f, 1f); // ease: fraction of remaining error per tick
        float dyaw = Mth.clamp(eyaw * k, -sp, sp);
        float dpit = Mth.clamp(epit * k, -sp, sp);

        float g = gcd(mc);
        if (Math.abs(dyaw) >= g) dyaw = Math.round(dyaw / g) * g; else dyaw = 0;
        if (Math.abs(dpit) >= g) dpit = Math.round(dpit / g) * g; else dpit = 0;

        p.setYRot(p.getYRot() + dyaw);
        p.setXRot(Mth.clamp(p.getXRot() + dpit, -90f, 90f));
        return Math.abs(Mth.wrapDegrees(ty - p.getYRot())) + Math.abs(tp - p.getXRot());
    }

    private static float gcd(Minecraft mc) {
        double f = mc.options.sensitivity().get() * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
