package com.arena.anticheat;

import com.arena.spawn.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-knockback (velocity). After a player is hit we watch how far they get
 * pushed away from the attacker at any point during the next few ticks
 * (extended by ping). Using the PEAK push, not the final position, so a
 * victim walking back toward the attacker isn't mistaken for no knockback.
 * Skipped when blocking, against walls, in water/webs/ladders, or re-hit.
 */
public class AntiKnockbackCheck implements Listener {

    private static final int BASE_TICKS = 6;

    private static class Pending {
        Location start;
        Vector dir;
        long created;
        double maxPush;
        double minPush;
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public AntiKnockbackCheck(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (e.getFinalDamage() <= 0) return;
        if (victim.isBlocking() || CheckUtil.movementExempt(victim)) return;
        if (!victim.getWorld().equals(attacker.getWorld())) return;

        Location start = victim.getLocation();
        if (CheckUtil.specialMedium(victim, start)) return;

        Vector dir = start.toVector().subtract(attacker.getLocation().toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize();
        if (blockedAhead(start, dir)) return;

        Pending pd = new Pending();
        pd.start = start;
        pd.dir = dir;
        pd.created = System.currentTimeMillis();
        pd.minPush = Settings.antiKbMinPush * (1.0 - 0.1 * netheritePieces(victim)); // netherite resists knockback
        UUID id = victim.getUniqueId();
        pending.put(id, pd); // a new hit replaces (and so cancels) the previous window

        int delay = BASE_TICKS + Math.min(victim.getPing(), 400) / 50;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pending.get(id) != pd) return;
            pending.remove(id);
            if (!victim.isOnline() || victim.isDead()) return;
            if (CheckUtil.lastTeleportTime(victim) > pd.created) return;
            if (pd.maxPush < pd.minPush) {
                ViolationManager.flag(victim, ViolationManager.CheckType.ANTI_KNOCKBACK, 2.0,
                        String.format("pushed only %.2f blocks after a hit", pd.maxPush));
            }
        }, delay);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Pending pd = pending.get(e.getPlayer().getUniqueId());
        Location to = e.getTo();
        if (pd == null || to == null || !to.getWorld().equals(pd.start.getWorld())) return;
        Vector moved = to.toVector().subtract(pd.start.toVector());
        moved.setY(0);
        pd.maxPush = Math.max(pd.maxPush, moved.dot(pd.dir));
    }

    private int netheritePieces(Player p) {
        int count = 0;
        for (var piece : p.getInventory().getArmorContents()) {
            if (piece != null && piece.getType().name().startsWith("NETHERITE_")) count++;
        }
        return count;
    }

    private boolean blockedAhead(Location start, Vector dir) {
        for (double dist : new double[]{0.7, 1.2}) {
            Location feet = start.clone().add(dir.clone().multiply(dist));
            if (!feet.getBlock().isPassable() || !feet.clone().add(0, 1, 0).getBlock().isPassable()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
