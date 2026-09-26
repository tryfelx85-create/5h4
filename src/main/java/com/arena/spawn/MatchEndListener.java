package com.arena.spawn;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/** Ends the match when a fighter dies or disconnects (a disconnect counts as a loss). Bot deaths: see BotListener. */
public class MatchEndListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        UUID loserId = loser.getUniqueId();
        if (!MatchManager.isFighting(loserId)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        MatchFlow.finish(opponent(loserId), loser, "death");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        UUID quitterId = quitter.getUniqueId();
        if (!MatchManager.isFighting(quitterId)) return;

        LivingEntity winner = opponent(quitterId);
        if (winner instanceof Player p) {
            p.sendMessage("§eYour opponent disconnected and forfeits the match.");
        }
        MatchFlow.finish(winner, quitter, "disconnect");
    }

    private LivingEntity opponent(UUID id) {
        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        UUID other = id.equals(p1) ? p2 : p1;
        return other != null ? BotManager.get(other) : null;
    }
}
