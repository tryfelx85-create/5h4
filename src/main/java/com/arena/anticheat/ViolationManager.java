package com.arena.anticheat;

import com.arena.spawn.FileLog;
import com.arena.spawn.Settings;
import com.arena.spawn.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Violation points per player per check. Points decay over time. When a
 * player's score for a check reaches the alert threshold, a warning about
 * POSSIBLE cheating is sent — only to TryFX and to players tagged Moders.
 * Nothing is ever kicked, banned or cancelled.
 */
public class ViolationManager implements Listener {

    public enum CheckType {
        REACH, SPEED, FLY, ANTI_KNOCKBACK, AIMBOT, KILLAURA
    }

    private static final Map<UUID, Map<CheckType, Double>> violations = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastAlert = new ConcurrentHashMap<>();
    private static Logger logger;

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
    }

    /** Adds points and, if the score is high enough, warns the staff recipients (rate-limited). */
    public static void flag(Player player, CheckType type, double points, String detail) {
        Map<CheckType, Double> playerMap = violations.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        double total = playerMap.merge(type, points, Double::sum);
        FileLog.write("anticheat.log", String.format("%s %s +%.1f (total %.1f) %s", player.getName(), type, points, total, detail));
        if (total < Settings.alertThreshold) return;

        String key = player.getUniqueId() + ":" + type;
        long now = System.currentTimeMillis();
        Long last = lastAlert.get(key);
        if (last != null && now - last < Settings.alertCooldownMs) return;
        lastAlert.put(key, now);

        String msg = String.format("§c[AC] §f%s §7possible cheating: §c%s §7(%s, score: %.1f)",
                player.getName(), type, detail, total);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.getName().equalsIgnoreCase(Settings.ownerNick) || TagManager.isModers(staff.getUniqueId())) {
                staff.sendMessage(msg);
            }
        }
        if (logger != null) {
            logger.warning("[AntiCheat] " + player.getName() + " possible " + type + " (" + detail + ", score " + total + ")");
        }
    }

    public static void decay(Player player, CheckType type, double amount) {
        Map<CheckType, Double> playerMap = violations.get(player.getUniqueId());
        if (playerMap == null) return;
        playerMap.computeIfPresent(type, (k, v) -> Math.max(0, v - amount));
    }

    public static double getViolations(Player player, CheckType type) {
        Map<CheckType, Double> playerMap = violations.get(player.getUniqueId());
        if (playerMap == null) return 0;
        return playerMap.getOrDefault(type, 0.0);
    }

    public static void reset(Player player) {
        violations.remove(player.getUniqueId());
        String prefix = player.getUniqueId() + ":";
        lastAlert.keySet().removeIf(k -> k.startsWith(prefix));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        reset(event.getPlayer());
    }
}
