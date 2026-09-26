package com.arena.spawn;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class VoteListener implements Listener {

    @EventHandler
    public void onVoteClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VoteInventoryHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        int kitChoice;
        switch (clicked.getType()) {
            case DIAMOND_SWORD -> kitChoice = 1;
            case DIAMOND_AXE -> kitChoice = 2;
            case SPLASH_POTION -> kitChoice = 3;
            case GOLDEN_APPLE -> kitChoice = 4;
            case NETHERITE_SWORD -> kitChoice = 5;
            default -> {
                return;
            }
        }

        VoteManager.castVote(player, kitChoice);

        // FIX: don't close the GUI here — the player may still want to change
        // their vote before the timer runs out or the other player agrees.
        // The GUI is closed automatically once the vote actually resolves
        // (see VoteManager.resolveVote).
        player.sendMessage("§7Vote registered — you can change it anytime before the match starts.");
    }
}
