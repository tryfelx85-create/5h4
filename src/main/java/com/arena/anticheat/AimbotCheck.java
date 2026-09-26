package com.arena.anticheat;

import com.arena.spawn.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aimbot. Two signals, both evaluated when a player hits another player:
 *  - "perfect aim": the crosshair is within a tiny angle of the exact hitbox
 *    centre on 8 hits in a row (humans land hits all over the hitbox).
 *  - "snap": a huge single-tick rotation immediately followed by a hit
 *    that lands almost dead-centre.
 */
public class AimbotCheck implements Listener {

    private static final int WINDOW = 8;
    private static final double SNAP_ERROR_DEG = 5;
    private static final long SNAP_WINDOW_MS = 100;

    private static class State {
        long rotTime;
        double peakRotation;
        final Deque<Double> errors = new ArrayDeque<>();
    }

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        double dYaw = CheckUtil.wrap180(to.getYaw() - from.getYaw());
        double dPitch = to.getPitch() - from.getPitch();
        double mag = Math.hypot(dYaw, dPitch);
        if (mag < 0.01) return;

        State s = states.computeIfAbsent(e.getPlayer().getUniqueId(), k -> new State());
        long now = System.currentTimeMillis();
        if (now - s.rotTime > SNAP_WINDOW_MS) s.peakRotation = 0;
        s.peakRotation = Math.max(s.peakRotation, mag);
        s.rotTime = now;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!attacker.getWorld().equals(victim.getWorld())) return;
        if (CheckUtil.distanceToBox(attacker, victim) < 1.0) return;

        State s = states.computeIfAbsent(attacker.getUniqueId(), k -> new State());
        double error = CheckUtil.angleTo(attacker, victim);
        long now = System.currentTimeMillis();

        if (s.peakRotation > Settings.aimbotSnapRotation && now - s.rotTime <= SNAP_WINDOW_MS && error < SNAP_ERROR_DEG) {
            ViolationManager.flag(attacker, ViolationManager.CheckType.AIMBOT, 2.0,
                    String.format("snap of %.0f deg then hit %.1f deg off-centre", s.peakRotation, error));
            s.peakRotation = 0;
        }

        s.errors.addLast(error);
        while (s.errors.size() > WINDOW) s.errors.removeFirst();
        if (s.errors.size() == WINDOW && s.errors.stream().allMatch(v -> v < Settings.aimbotPerfectError)) {
            ViolationManager.flag(attacker, ViolationManager.CheckType.AIMBOT, 3.0,
                    "perfect aim on " + WINDOW + " consecutive hits");
            s.errors.clear();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        states.remove(e.getPlayer().getUniqueId());
    }
}
