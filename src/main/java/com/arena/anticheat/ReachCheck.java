package com.arena.anticheat;

import com.arena.spawn.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Melee hits from too far away. Measures eye-to-hitbox distance (what the
 * game itself uses), with a base buffer plus a latency allowance because the
 * server sees both players slightly behind where the attacker's client did.
 */
public class ReachCheck implements Listener {

    private static final double VANILLA_REACH = 3.0;
    private static final double MAX_PING_MS = 250;
    private static final double MOVE_SPEED_BPS = 5.6; // max relative drift per second of latency

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!attacker.getWorld().equals(victim.getWorld())) return;

        double ping = Math.min(Math.max(attacker.getPing(), victim.getPing()), MAX_PING_MS);
        double limit = VANILLA_REACH + Settings.reachBuffer + (ping / 1000.0) * MOVE_SPEED_BPS;
        double distance = CheckUtil.distanceToBox(attacker, victim);

        if (distance > limit) {
            double points = Math.min(1.0 + (distance - limit) * 3.0, 5.0);
            ViolationManager.flag(attacker, ViolationManager.CheckType.REACH, points,
                    String.format("%.2f blocks, limit %.2f", distance, limit));
        }
    }
}
