package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the arena clean. Fighters with the UHC kit may build (blocks, webs, water, lava),
 * but every block they place or spill (including flowing liquid) is remembered, they may
 * only break/scoop those, and everything is restored when the match ends. Items they drop,
 * items from blocks they break and projectiles they launch (arrows, pearls, potions)
 * are removed from the world at the end of the match too.
 */
public class ArenaBlockListener implements Listener {

    private static final Map<Location, BlockState> tracked = new LinkedHashMap<>();
    private static final Set<UUID> spawnedEntities = new HashSet<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!MatchManager.isFighting(e.getPlayer().getUniqueId())) return;
        tracked.putIfAbsent(e.getBlock().getLocation(), e.getBlockReplacedState());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!MatchManager.isFighting(e.getPlayer().getUniqueId())) return;
        tracked.putIfAbsent(e.getBlock().getLocation(), e.getBlock().getState());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        if (MatchManager.isFighting(p.getUniqueId()) && !tracked.containsKey(e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (MatchManager.isFighting(p.getUniqueId()) && !tracked.containsKey(e.getBlock().getLocation())) {
            e.setCancelled(true);
            p.sendMessage("§cYou can only break blocks placed during this match.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (!MatchManager.isMatchActive()) return;
        if (tracked.containsKey(e.getBlock().getLocation())) {
            tracked.putIfAbsent(e.getToBlock().getLocation(), e.getToBlock().getState());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (MatchManager.isFighting(e.getPlayer().getUniqueId())) {
            spawnedEntities.add(e.getItemDrop().getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        if (!MatchManager.isFighting(e.getPlayer().getUniqueId())) return;
        for (Item item : e.getItems()) {
            spawnedEntities.add(item.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player p && MatchManager.isFighting(p.getUniqueId())) {
            spawnedEntities.add(e.getEntity().getUniqueId());
        }
    }

    /** Puts every touched block back (newest first) and removes every item/projectile spawned during the match. */
    public static void restoreAll() {
        List<BlockState> states = new ArrayList<>(tracked.values());
        tracked.clear();
        for (int i = states.size() - 1; i >= 0; i--) {
            states.get(i).update(true, false);
        }

        for (UUID id : spawnedEntities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
        spawnedEntities.clear();
    }
}
