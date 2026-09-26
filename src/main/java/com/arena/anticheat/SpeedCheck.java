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
 * Horizontal speed. Distance is averaged over a ~1.5s window (not per tick)
 * so lag bursts don't cause false flags. Skipped for ~1.5s after knockback,
 * damage or teleports, on ice, while flying/gliding/riding, and it scales
 * the limit with the Speed potion effect and the player's ping.
 */
public class SpeedCheck implements Listener {

    private static final double LEEWAY = 1.25;
    private static final long WINDOW_MS = 1500;
    private static final long GAP_RESET_MS = 300;

    private static class State {
        long windowStart;
        long lastEvent;
        double distance;
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
                || onIce(to)) {
            reset(s);
            return;
        }

        long now = System.currentTimeMillis();
        if (s.windowStart == 0 || now - s.lastEvent > GAP_RESET_MS) {
            s.windowStart = now;
            s.distance = 0;
        }
        s.lastEvent = now;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        s.distance += Math.sqrt(dx * dx + dz * dz);

        long elapsed = now - s.windowStart;
        if (elapsed >= WINDOW_MS) {
            double bps = s.distance / (elapsed / 1000.0);
            double limit = maxBps(p);
            if (bps > limit) {
                ViolationManager.flag(p, ViolationManager.CheckType.SPEED, 2.0,
                        String.format("%.1f b/s, limit %.1f", bps, limit));
            }
            s.windowStart = now;
            s.distance = 0;
        }
    }

    private double maxBps(Player p) {
        double limit = Settings.speedMaxBps * LEEWAY;
        PotionEffect speed = p.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            limit *= 1.0 + 0.2 * (speed.getAmplifier() + 1);
        }
        return limit * (1.0 + Math.min(p.getPing(), 300) / 1500.0);
    }

    private boolean onIce(Location loc) {
        String below = loc.clone().subtract(0, 0.5, 0).getBlock().getType().name();
        String feet = loc.getBlock().getType().name();
        return below.contains("ICE") || feet.contains("ICE");
    }

    private void reset(State s) {
        s.windowStart = 0;
        s.distance = 0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.remove(e.getPlayer().getUniqueId());
    }
}
