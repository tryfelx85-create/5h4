package com.arena.spawn;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Right-side sidebar HUD shown to everyone while a match is running:
 * both fighters' names, HP and ping (players only), plus the time left.
 * Also puts each fighter's HP next to their nickname (bots: in their name tag).
 * Uses the main scoreboard so the tag prefixes (which live there too) keep working.
 */
public class FightHud {

    private static final String OBJECTIVE = "arena_hud";
    private static final Set<UUID> suffixed = new HashSet<>();

    public static void start(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, FightHud::update, 0L, 5L);
    }

    public static void clearSuffixes() {
        for (UUID id : suffixed) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) TagManager.setSuffix(p, "");
        }
        suffixed.clear();
    }

    public static void remove() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective old = board.getObjective(OBJECTIVE);
        if (old != null) old.unregister();
    }

    private static void update() {
        UUID id1 = MatchManager.getPlayer1();
        UUID id2 = MatchManager.getPlayer2();
        LivingEntity e1 = id1 != null ? BotManager.get(id1) : null;
        LivingEntity e2 = id2 != null ? BotManager.get(id2) : null;

        if (!MatchManager.isMatchActive() || e1 == null || e2 == null) {
            remove();
            clearSuffixes();
            return;
        }

        for (LivingEntity e : new LivingEntity[]{e1, e2}) {
            if (e instanceof Player p) {
                TagManager.setSuffix(p, " §c❤" + String.format("%.1f", Math.max(0, p.getHealth())));
                suffixed.add(p.getUniqueId());
            } else {
                BotManager.refreshName(e.getUniqueId());
            }
        }

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        remove();
        Objective obj = board.registerNewObjective(OBJECTIVE, "dummy", Component.text("§c§l⚔ FIGHT ⚔"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§6§lnis senim").setScore(7);
        obj.getScore("§e" + BotManager.nameOf(e1)).setScore(6);
        obj.getScore(statLine(e1, "§1")).setScore(5);
        obj.getScore("§r").setScore(4);
        obj.getScore("§b" + BotManager.nameOf(e2)).setScore(3);
        obj.getScore(statLine(e2, "§2")).setScore(2);

        int left = MatchFlow.getSecondsLeft();
        if (left > 0 && MatchManager.isFightStarted()) {
            obj.getScore(String.format("§8⏱ §7%d:%02d", left / 60, left % 60)).setScore(1);
        }
    }

    private static String statLine(LivingEntity e, String uniq) {
        double hp = Math.max(0, e.getHealth());
        String ping = e instanceof Player p ? p.getPing() + "ms" : "bot";
        return uniq + "§c❤ " + String.format("%.1f", hp) + "§7/" + (int) e.getMaxHealth() + " §8| §a" + ping;
    }
}
