package com.arena.spawn;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

/** Applies the tag on join and shows it in chat before the player's name. */
public class TagListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        TagManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        // While a match runs, fighters only see chat from each other, Master and Moders.
        // Everyone else (including spectators) still sees every message.
        UUID sender = event.getPlayer().getUniqueId();
        boolean fighterVisible = MatchManager.isFighting(sender)
                || TagManager.isMaster(sender)
                || TagManager.isModers(sender);
        if (MatchManager.isMatchActive() && !fighterVisible) {
            event.viewers().removeIf(v -> v instanceof Player p && MatchManager.isFighting(p.getUniqueId()));
        }

        event.renderer((source, sourceDisplayName, message, viewer) -> {
            TagManager.PlayerTag tag = TagManager.getTag(source.getUniqueId());
            String text = tag != null ? tag.text : "0";
            String color = tag != null ? TagManager.colorFor(tag) : "§7";
            return Component.empty()
                    .append(LegacyComponentSerializer.legacySection().deserialize(color + "[" + text + "] §r"))
                    .append(sourceDisplayName)
                    .append(Component.text(": "))
                    .append(message);
        });
    }
}
