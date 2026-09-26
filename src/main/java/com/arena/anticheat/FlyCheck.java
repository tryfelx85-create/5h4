package com.arena.anticheat;

import com.arena.spawn.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flight. Uses block-based ground detection (not the client's onGround flag) and flags:
 *  - hovering / not falling after being airborne long enough that a jump must have peaked
 *  - rising higher above the last ground than any jump (with Jump Boost) allows
 */
public class FlyCheck implements Listener {

    private static final int MIN_AIR_TICKS = 10;      // a jump peaks after ~6 ticks
    private static final double MIN_FALL_DY = -0.05;  // by then the player must be clearly falling
    private static final int HOVER_STREAK = 5;

    private static class State {
        int airTicks;
        int hover;
        double groundY = Double.NaN;
    }

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!e.hasChangedPosition()) return;
        Player p = e.getPlayer();
        State s = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        Location from = e.getFrom();
        Location to = e.getTo();

        if (to == null || !from.getWorld().equals(to.getWorld())
                || CheckUtil.movementExempt(p)
                || CheckUtil.recentlyAffected(p, 1500)
                || CheckUtil.specialMedium(p, to)
                || p.hasPotionEffect(PotionEffectType.LEVITATION)
                || p.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            reset(s);
            return;
        }

        if (CheckUtil.onSolid(to)) {
            s.airTicks = 0;
            s.hover = 0;
            s.groundY = to.getY();
            return;
        }

        s.airTicks++;
        double dy = to.getY() - from.getY();

        if (s.airTicks >= MIN_AIR_TICKS && dy > MIN_FALL_DY) {
            if (++s.hover >= HOVER_STREAK) {
                ViolationManager.flag(p, ViolationManager.CheckType.FLY, 2.5,
                        String.format("hovering, dy %.3f after %d air ticks", dy, s.airTicks));
                s.hover = 0;
            }
        } else {
            s.hover = 0;
        }

        if (!Double.isNaN(s.groundY)) {
            PotionEffect jump = p.getPotionEffect(PotionEffectType.JUMP_BOOST);
            double maxRise = Settings.flyMaxRise + (jump != null ? 0.6 * (jump.getAmplifier() + 1) : 0);
            double rise = to.getY() - s.groundY;
            if (rise > maxRise) {
                ViolationManager.flag(p, ViolationManager.CheckType.FLY, 3.0,
                        String.format("rose %.2f blocks in the air, max %.2f", rise, maxRise));
                s.groundY = to.getY(); // don't re-flag every tick of the same climb
            }
        }
    }

    private void reset(State s) {
        s.airTicks = 0;
        s.hover = 0;
        s.groundY = Double.NaN;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.remove(e.getPlayer().getUniqueId());
    }
}
