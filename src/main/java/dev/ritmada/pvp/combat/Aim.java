// Java 17+, eased rotation: hysteresis start/stop, low-passed step, slow lerped aim wobble, pitch dead zone
package dev.ritmada.pvp.combat;

import dev.ritmada.pvp.config.ProfileSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

final class Aim {
    private static final Random RNG = new Random();
    private static Vec3 off = Vec3.ZERO, offTarget = Vec3.ZERO;
    private static int lastWobble;
    private static boolean tracking;
    private static float prevYaw, prevPit;

    private Aim() {}

    /** Steps rotation toward pt, returns remaining angular error in degrees. */
    static float at(Minecraft mc, LocalPlayer p, Vec3 pt, ProfileSettings s) {
        // wobble: new offset every ~25 ticks, camera glides to it instead of twitching
        if (s.jitter > 0f) {
            if (p.tickCount - lastWobble > 25) {
                double j = s.jitter * 0.08;
                offTarget = new Vec3((RNG.nextDouble() - .5) * 2 * j, (RNG.nextDouble() - .5) * 2 * j, (RNG.nextDouble() - .5) * 2 * j);
                lastWobble = p.tickCount;
            }
        } else offTarget = Vec3.ZERO;
        off = off.lerp(offTarget, 0.08);
        pt = pt.add(off);

        Vec3 e = p.getEyePosition();
        double dx = pt.x - e.x, dy = pt.y - e.y, dz = pt.z - e.z;
        float ty = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tp = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        float eyaw = Mth.wrapDegrees(ty - p.getYRot());
        float epit = tp - p.getXRot();
        float err = Math.abs(eyaw) + Math.abs(epit);

        // hysteresis: start correcting past 3 deg, stop under 1 deg (a single threshold makes it flutter at the edge)
        if (tracking) { if (err < 1.0f) tracking = false; } else if (err > 3.0f) tracking = true;
        if (!tracking) { prevYaw = 0; prevPit = 0; return err; }

        float sp = Math.max(s.aimSpeed, 0.5f);
        float k = Mth.clamp(sp / 60f, 0.08f, 1f);
        float dyaw = Mth.clamp(eyaw * k, -sp, sp);
        float dpit = Math.abs(epit) < 2.5f ? 0f : Mth.clamp(epit * k, -sp, sp); // body is tall, ignore small pitch error

        // low-pass the step so speed ramps up and down instead of stepping tick to tick
        dyaw = prevYaw + (dyaw - prevYaw) * 0.6f;
        dpit = prevPit + (dpit - prevPit) * 0.6f;
        prevYaw = dyaw; prevPit = dpit;

        float g = gcd(mc);
        dyaw = Math.abs(dyaw) >= g ? Math.round(dyaw / g) * g : 0;
        dpit = Math.abs(dpit) >= g ? Math.round(dpit / g) * g : 0;

        p.setYRot(p.getYRot() + dyaw);
        p.setXRot(Mth.clamp(p.getXRot() + dpit, -90f, 90f));
        return Math.abs(Mth.wrapDegrees(ty - p.getYRot())) + Math.abs(tp - p.getXRot());
    }

    private static float gcd(Minecraft mc) {
        double f = mc.options.sensitivity().get() * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
