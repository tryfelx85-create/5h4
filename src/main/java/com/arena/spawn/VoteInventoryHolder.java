package com.arena.spawn;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder so VoteListener/VoteGUI can identify the kit-vote inventory
 * apart from any other inventory a player might have open.
 */
public class VoteInventoryHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
