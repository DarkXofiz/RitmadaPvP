// Java 17+, auto PvP brain: target pick, profile-gated melee, per-mode actions
package dev.ritmada.pvp.combat;

import dev.ritmada.pvp.config.Config;
import dev.ritmada.pvp.config.Profile;
import dev.ritmada.pvp.config.ProfileSettings;
import dev.ritmada.pvp.config.PvpMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.Random;

public final class AutoPvp {
    // vanilla survival entity reach, fixed on purpose
    private static final float REACH = 3.0f;
    private static final Random RNG = new Random();
    private static int cd, rocketCd, reaction;
    private static boolean hadTarget, jumpHeld, strafed, jumpNext, moved;
    private static int retreat;
    private static final int RETREAT_TICKS = 5;
    private static int hurtPrev;
    private static int strafeT, strafeDir;

    private AutoPvp() {}

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null || !Config.enabled || mc.screen != null
                || (!Config.allowServers && !mc.isLocalServer())) {
            releaseKeys(mc);
            hadTarget = false;
            return;
        }
        if (cd > 0) cd--;
        if (rocketCd > 0) rocketCd--;

        ProfileSettings s = Config.cur();
        int ht = p.hurtTime;
        boolean hurtEdge = ht > hurtPrev; // fresh hit taken this tick
        hurtPrev = ht;
        LivingEntity t = pick(mc, p);
        if (t == null) { releaseKeys(mc); hadTarget = false; return; }

        // Beast: take a hit from a nearby player -> jump exactly once
        if (hurtEdge && Config.mode == PvpMode.BEAST && Config.param(PvpMode.BEAST) >= 0.5f && p.distanceTo(t) < 6f)
            jumpNext = true;

        if (!hadTarget) { hadTarget = true; reaction = Math.round(s.reactionTicks); }
        if (reaction > 0) { reaction--; return; }

        if (Config.mode == PvpMode.POT && heal(mc, p, s)) return;
        if (Config.mode == PvpMode.ELYTRA) rocket(mc, p);
        if (Config.mode == PvpMode.CRYSTAL && crystal(mc, p, t, s)) return;
        melee(mc, p, t, s);
    }

    private static LivingEntity pick(Minecraft mc, LocalPlayer p) {
        double range = Config.mode == PvpMode.ELYTRA ? 24 : 8;
        double best = range * range;
        LivingEntity out = null;
        for (Player o : mc.level.players()) {
            // FILL: friend list filter here (names -> skip)
            if (o == p || !o.isAlive() || o.isSpectator() || p.isAlliedTo(o)) continue;
            double d = p.distanceToSqr(o);
            if (d < best) { best = d; out = o; }
        }
        return out;
    }

    private static void melee(Minecraft mc, LocalPlayer p, LivingEntity t, ProfileSettings s) {
        if (Config.mode == PvpMode.BEAST || Config.mode == PvpMode.POT) { strafe(mc); spacing(mc, p, t); }
        Aim.at(mc, p, t.position().add(0, t.getBbHeight() * 0.7, 0), s); // chest, steadier than box center while the target hops

        boolean ready = p.getAttackStrengthScale(0.5f) >= s.minCooldown;
        boolean beast = Config.mode == PvpMode.BEAST;
        boolean pot = Config.mode == PvpMode.POT;
        boolean wantCrit = !beast; // Pot hops and hits on the way down, Beast stays grounded // Beast hits on the ground, never hops to crit
        boolean falling = p.fallDistance > 0f && !p.onGround() && !p.isInWater() && !p.onClimbable();

        // crit = hit while falling; hop when cooldown is ready and target is close
        boolean hop = beast
                ? jumpNext && p.onGround()
                : wantCrit && ready && p.onGround() && p.distanceTo(t) < REACH + 1.0f;
        if (beast && hop) jumpNext = false; // one jump per hit taken
        mc.options.keyJump.setDown(hop);
        jumpHeld = hop;

        if (!ready) return;
        if (wantCrit && !falling) return;

        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1.0f).scale(REACH));
        Optional<Vec3> hit = t.getBoundingBox().inflate(t.getPickRadius()).clip(eye, end);
        if (hit.isEmpty()) return;

        if (RNG.nextFloat() < s.missChance) { reaction = 1 + RNG.nextInt(4); return; }
        mc.gameMode.attack(p, t);
        p.swing(InteractionHand.MAIN_HAND);
        // Beast: step back right after the hit so the counter-swing lands on air
        if ((beast || pot) && Config.profile != Profile.NOOB) retreat = RETREAT_TICKS;
    }

    private static boolean heal(Minecraft mc, LocalPlayer p, ProfileSettings s) {
        if (cd > 0 || p.getHealth() > Config.param(PvpMode.POT)) return false;
        if (!hasMatching(p, AutoPvp::isHealing)) return false;
        // back off while healing instead of standing in melee range
        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(true);
        moved = true;
        if (jumpHeld) { mc.options.keyJump.setDown(false); jumpHeld = false; }
        Vec3 l = p.getLookAngle();
        Vec3 down = p.getEyePosition().add(l.x * 0.3, -2.5, l.z * 0.3); // look down gradually, no instant 90 degree snap
        if (Aim.at(mc, p, down, s) > 12f) return true;
        withMatching(p, AutoPvp::isHealing, () -> mc.gameMode.useItem(p, InteractionHand.MAIN_HAND));
        cd = 10;
        return true;
    }

    private static void rocket(Minecraft mc, LocalPlayer p) {
        if (!p.isFallFlying() || rocketCd > 0) return;
        if (withSlot(p, Items.FIREWORK_ROCKET, () -> mc.gameMode.useItem(p, InteractionHand.MAIN_HAND)))
            rocketCd = (int) Config.param(PvpMode.ELYTRA);
    }

    private static boolean crystal(Minecraft mc, LocalPlayer p, LivingEntity t, ProfileSettings s) {
        if (cd > 0) return false;
        int delay = (int) Config.param(PvpMode.CRYSTAL);

        // break: nearest crystal around target that we can reach
        EndCrystal bestC = null;
        double bd = Double.MAX_VALUE;
        for (EndCrystal c : mc.level.getEntitiesOfClass(EndCrystal.class, t.getBoundingBox().inflate(6))) {
            double d = p.distanceTo(c);
            if (d <= REACH && d < bd) { bd = d; bestC = c; }
        }
        if (bestC != null) {
            if (Aim.at(mc, p, bestC.getBoundingBox().getCenter(), s) < 12f) {
                mc.gameMode.attack(p, bestC);
                p.swing(InteractionHand.MAIN_HAND);
                cd = delay;
            }
            return true;
        }

        // place: obsidian/bedrock near target's feet with 2 free blocks above
        BlockPos feet = t.blockPosition().below();
        BlockPos base = null;
        for (int dx = -1; dx <= 1 && base == null; dx++) {
            for (int dz = -1; dz <= 1 && base == null; dz++) {
                BlockPos b = feet.offset(dx, 0, dz);
                var st = mc.level.getBlockState(b);
                if (!(st.is(Blocks.OBSIDIAN) || st.is(Blocks.BEDROCK))) continue;
                if (!mc.level.isEmptyBlock(b.above()) || !mc.level.isEmptyBlock(b.above(2))) continue;
                if (!mc.level.getEntities(null, new AABB(b.above()).expandTowards(0, 1, 0)).isEmpty()) continue;
                if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(b)) > 16) continue;
                base = b;
            }
        }
        if (base == null) return false;

        Vec3 face = Vec3.atCenterOf(base).add(0, 0.5, 0);
        // anticheat: placing without facing the block trips rotation checks, so aim first and place inside the cone
        if (Aim.at(mc, p, face, s) > 10f) return true;
        final BlockPos fb = base;
        boolean ok = withSlot(p, Items.END_CRYSTAL, () ->
                mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(face, Direction.UP, fb, false)));
        if (ok) cd = delay;
        return ok;
    }

    /** Swap to the first hotbar slot holding item, run action, swap back. */
    private static boolean withSlot(LocalPlayer p, Item item, Runnable action) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).is(item)) { slot = i; break; }
        }
        if (slot < 0) return false;
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        action.run();
        p.getInventory().selected = prev;
        return true;
    }

    // splash healing / strong healing only; description id ends in "...effect.healing" or "...effect.strong_healing" on all supported versions
    private static boolean isHealing(ItemStack st) {
        return st.is(Items.SPLASH_POTION) && st.getDescriptionId().contains("healing");
    }

    private static boolean hasMatching(LocalPlayer p, Predicate<ItemStack> f) {
        for (int i = 0; i < 9; i++) if (f.test(p.getInventory().getItem(i))) return true;
        return false;
    }

    private static boolean withMatching(LocalPlayer p, Predicate<ItemStack> f, Runnable action) {
        for (int i = 0; i < 9; i++) {
            if (!f.test(p.getInventory().getItem(i))) continue;
            int prev = p.getInventory().selected;
            p.getInventory().selected = i;
            action.run();
            p.getInventory().selected = prev;
            return true;
        }
        return false;
    }

    private static boolean hasHotbar(LocalPlayer p, Item item) {
        for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(item)) return true;
        return false;
    }

    // Beast: strafe left/right in short random bursts (Noob stands still)
    private static void strafe(Minecraft mc) {
        if (Config.profile == Profile.NOOB) return;
        if (--strafeT <= 0) {
            strafeDir = RNG.nextInt(3) - 1;
            strafeT = 8 + RNG.nextInt(Config.profile == Profile.PRO ? 16 : 8);
        }
        mc.options.keyLeft.setDown(strafeDir < 0);
        mc.options.keyRight.setDown(strafeDir > 0);
        strafed = true;
    }

    // Beast spacing: close to reach, hold the gap, back off after each hit
    private static void spacing(Minecraft mc, LocalPlayer p, LivingEntity t) {
        double d = p.distanceTo(t);
        boolean fwd = false, back = false;
        if (Config.profile == Profile.NOOB) {
            fwd = d > REACH;
        } else if (retreat > 0) {
            retreat--; back = true;
        } else if (d > REACH) {
            fwd = true;
        } else if (d < 2.2) {
            back = true;
        }
        mc.options.keyUp.setDown(fwd);
        mc.options.keyDown.setDown(back);
        moved = true;
    }

    private static void releaseKeys(Minecraft mc) {
        if (moved) { mc.options.keyUp.setDown(false); mc.options.keyDown.setDown(false); moved = false; retreat = 0; }
        jumpNext = false;
        if (jumpHeld) { mc.options.keyJump.setDown(false); jumpHeld = false; }
        if (strafed) { mc.options.keyLeft.setDown(false); mc.options.keyRight.setDown(false); strafed = false; }
    }
}
