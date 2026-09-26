package com.arena.spawn;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

/** A bot that dies while fighting loses the match; either way it is then removed from the bot list. */
public class BotListener implements Listener {

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        UUID id = dead.getUniqueId();
        if (!BotManager.isBot(id)) return;

        e.getDrops().clear();
        e.setDroppedExp(0);

        if (MatchManager.isFighting(id)) {
            UUID p1 = MatchManager.getPlayer1();
            UUID p2 = MatchManager.getPlayer2();
            UUID otherId = id.equals(p1) ? p2 : p1;
            LivingEntity winner = otherId != null ? BotManager.get(otherId) : null;
            MatchFlow.finish(winner, dead, "bot defeated");
        }
        BotManager.forget(id);
    }
}
