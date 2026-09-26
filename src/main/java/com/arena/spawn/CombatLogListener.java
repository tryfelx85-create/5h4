package com.arena.spawn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Writes every landed hit between the two fighters to combat.log, for reviewing disputes. */
public class CombatLogListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!MatchManager.isFighting(attacker.getUniqueId()) || !MatchManager.isFighting(victim.getUniqueId())) return;

        double hpAfter = Math.max(0, victim.getHealth() - e.getFinalDamage());
        FileLog.write("combat.log", String.format("HIT %s -> %s dmg=%.1f victimHp=%.1f dist=%.2f pingA=%d pingV=%d",
                attacker.getName(), victim.getName(), e.getFinalDamage(), hpAfter,
                attacker.getLocation().distance(victim.getLocation()), attacker.getPing(), victim.getPing()));
    }
}
