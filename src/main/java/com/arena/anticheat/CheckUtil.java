package com.arena.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared helpers and "something external just moved this player" tracking used by all checks. */
public class CheckUtil implements Listener {

    private static final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastVelocity = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastDamage = new ConcurrentHashMap<>();

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        lastTeleport.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onVelocity(PlayerVelocityEvent e) {
        lastVelocity.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            lastDamage.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        lastTeleport.remove(id);
        lastVelocity.remove(id);
        lastDamage.remove(id);
    }

    public static long lastTeleportTime(Player p) {
        return lastTeleport.getOrDefault(p.getUniqueId(), 0L);
    }

    /** True if the player was teleported, given velocity (knockback etc.) or damaged within the last ms. */
    public static boolean recentlyAffected(Player p, long ms) {
        long now = System.currentTimeMillis();
        UUID id = p.getUniqueId();
        return now - lastTeleport.getOrDefault(id, 0L) < ms
                || now - lastVelocity.getOrDefault(id, 0L) < ms
                || now - lastDamage.getOrDefault(id, 0L) < ms;
    }

    /** Players who may legitimately move in non-standard ways. */
    public static boolean movementExempt(Player p) {
        GameMode gm = p.getGameMode();
        if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return true;
        return p.isDead() || p.getAllowFlight() || p.isFlying() || p.isGliding()
                || p.isInsideVehicle() || p.isRiptiding();
    }

    /** Water, lava, ladders, webs and similar media where normal movement rules don't apply. */
    public static boolean specialMedium(Player p, Location loc) {
        if (p.isInWater() || p.isInLava() || p.isClimbing()) return true;
        Material m = loc.getBlock().getType();
        if (m == Material.COBWEB || m == Material.SWEET_BERRY_BUSH || m == Material.POWDER_SNOW
                || m == Material.BUBBLE_COLUMN || Tag.CLIMBABLE.isTagged(m)) {
            return true;
        }
        Material below = loc.clone().subtract(0, 1, 0).getBlock().getType();
        return below == Material.SLIME_BLOCK || Tag.BEDS.isTagged(below) || below == Material.HONEY_BLOCK;
    }

    /** Block-based ground check (does not trust the client's onGround flag). */
    public static boolean onSolid(Location loc) {
        World w = loc.getWorld();
        if (w == null) return false;
        double[] offs = {-0.3, 0, 0.3};
        for (double dx : offs) {
            for (double dz : offs) {
                Block b = w.getBlockAt(
                        (int) Math.floor(loc.getX() + dx),
                        (int) Math.floor(loc.getY() - 0.2),
                        (int) Math.floor(loc.getZ() + dz));
                if (!b.isPassable()) return true;
            }
        }
        return !loc.getBlock().isPassable();
    }

    /** Distance from the attacker's eyes to the nearest point of the target's hitbox. */
    public static double distanceToBox(Player attacker, Entity target) {
        Vector e = attacker.getEyeLocation().toVector();
        BoundingBox box = target.getBoundingBox();
        double cx = Math.max(box.getMinX(), Math.min(e.getX(), box.getMaxX()));
        double cy = Math.max(box.getMinY(), Math.min(e.getY(), box.getMaxY()));
        double cz = Math.max(box.getMinZ(), Math.min(e.getZ(), box.getMaxZ()));
        double dx = e.getX() - cx, dy = e.getY() - cy, dz = e.getZ() - cz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Angle in degrees between where the attacker looks and the centre of the target's hitbox. */
    public static double angleTo(Player attacker, Entity target) {
        Location eye = attacker.getEyeLocation();
        Vector toTarget = target.getBoundingBox().getCenter().subtract(eye.toVector());
        if (toTarget.lengthSquared() < 1.0E-6) return 0;
        return Math.toDegrees(eye.getDirection().angle(toTarget));
    }

    public static double wrap180(double angle) {
        angle %= 360;
        if (angle >= 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
}
