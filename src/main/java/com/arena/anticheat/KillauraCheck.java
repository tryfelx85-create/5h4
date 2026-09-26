package com.arena.anticheat;

import com.arena.spawn.Settings;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Killaura. Flags hits that a human cannot land:
 *  - hitting a target who is far outside the attacker's field of view
 *  - hitting through solid blocks (both the target's centre and eyes are behind a wall)
 */
public class KillauraCheck implements Listener {

    private static final double MIN_DISTANCE_FOR_ANGLE = 1.5;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!attacker.getWorld().equals(victim.getWorld())) return;

        double distance = CheckUtil.distanceToBox(attacker, victim);

        if (distance > MIN_DISTANCE_FOR_ANGLE) {
            double angle = CheckUtil.angleTo(attacker, victim);
            if (angle > Settings.killauraMaxAngle) {
                ViolationManager.flag(attacker, ViolationManager.CheckType.KILLAURA, 3.0,
                        String.format("hit a target %.0f deg outside their view", angle));
            }
        }

        if (blocked(attacker, victim.getBoundingBox().getCenter())
                && blocked(attacker, victim.getEyeLocation().toVector())) {
            ViolationManager.flag(attacker, ViolationManager.CheckType.KILLAURA, 3.0, "hit through a wall");
        }
    }

    private boolean blocked(Player attacker, Vector target) {
        Location eye = attacker.getEyeLocation();
        Vector dir = target.clone().subtract(eye.toVector());
        double length = dir.length();
        if (length < 1.0) return false;
        World world = eye.getWorld();
        return world.rayTraceBlocks(eye, dir.normalize(), length, FluidCollisionMode.NEVER, true) != null;
    }
}
