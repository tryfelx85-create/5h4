package com.arena.spawn;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvPRestrictionListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity attacker = event.getDamager();

        boolean victimBot = BotManager.isBot(victim.getUniqueId());
        boolean victimFighter = victim instanceof Player || victimBot;
        boolean attackerFighter = attacker instanceof Player || BotManager.isBot(attacker.getUniqueId());
        if (!victimFighter || !attackerFighter) return;

        // A bot that is not in a match cannot be hurt at all (PvP is off for lobby bots)
        if (victimBot && !MatchManager.isFighting(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Otherwise only the two fighters may hurt each other, and only once the fight has really begun
        if (!MatchManager.isFighting(victim.getUniqueId()) || !MatchManager.isFighting(attacker.getUniqueId())
                || !MatchManager.isFightStarted() || MatchManager.isPaused()) {
            event.setCancelled(true);
        }
    }
}
