package com.arena.spawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Everything that happens after the kit vote: the freeze + countdown, the
 * match time limit, and the single place where a match ends (win, loss by
 * disconnect, time-out, draw). Fighters may be players or bots.
 */
public class MatchFlow {

    private static ArenaPlugin plugin;
    private static BukkitTask countdownTask;
    private static BukkitTask limitTask;
    private static int secondsLeft;

    public static void init(ArenaPlugin instance) {
        plugin = instance;
    }

    public static int getSecondsLeft() {
        return limitTask != null ? secondsLeft : 0;
    }

    /** Full health, no fire, no potion effects (and full food for players). */
    public static void resetFighter(LivingEntity e) {
        if (e == null || e.isDead()) return;
        e.setHealth(e.getMaxHealth());
        e.setFireTicks(0);
        e.setFallDistance(0f);
        for (PotionEffect effect : new ArrayList<>(e.getActivePotionEffects())) {
            e.removePotionEffect(effect.getType());
        }
        if (e instanceof Player p) {
            p.setFoodLevel(20);
            p.setSaturation(20f);
        }
    }

    /** Freezes both fighters, counts down 3-2-1, then lets the fight begin. */
    public static void beginCountdown() {
        cancelTasks();
        MatchManager.setFrozen(true);
        countdownTask = new BukkitRunnable() {
            int n = Settings.countdownSeconds;

            @Override
            public void run() {
                if (!MatchManager.isMatchActive()) {
                    cancel();
                    return;
                }
                if (n > 0) {
                    titleToFighters("§e" + n);
                    n--;
                    return;
                }
                cancel();
                countdownTask = null;
                MatchManager.setFrozen(false);
                MatchManager.setFightStarted(true);
                // Vanilla PvP-server hunger: full food bar but no saturation, so health only comes back slowly
                for (Player p : fighters()) {
                    if (p == null) continue;
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.SATURATION);
                    p.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
                    p.setFoodLevel(20);
                    p.setSaturation(0f);
                    p.setExhaustion(0f);
                }
                for (Player p : fighters()) {
                    if (p == null) continue;
                    p.sendMessage("§c§lFIGHT!");
                }
                titleToFighters("§c§lFIGHT!");
                startLimitTimer();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static void startLimitTimer() {
        secondsLeft = Settings.matchTimeLimit;
        if (secondsLeft <= 0) return;
        limitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!MatchManager.isMatchActive()) {
                cancelTasks();
                return;
            }
            if (MatchManager.isPaused()) return;
            if (--secondsLeft <= 0) {
                timeUp();
            }
        }, 20L, 20L);
    }

    private static void timeUp() {
        LivingEntity[] f = entities();
        if (f[0] == null || f[1] == null) {
            draw("Time is up.");
            return;
        }
        double h1 = f[0].getHealth();
        double h2 = f[1].getHealth();
        if (Math.abs(h1 - h2) < 0.01) {
            draw("Time is up and both fighters have equal health - it's a draw.");
        } else if (h1 > h2) {
            finish(f[0], f[1], "time limit, more health");
        } else {
            finish(f[1], f[0], "time limit, more health");
        }
    }

    /** Ends the match with a winner. Either fighter may be null (offline) and may be a bot. */
    public static void finish(LivingEntity winner, LivingEntity loser, String reason) {
        cancelTasks();

        tell(winner, "§a§lYou won the match!");
        tell(loser, "§c§lYou lost the match.");
        if (winner instanceof Player p) TagManager.recordWin(p);
        else if (winner != null) TagManager.recordWin(winner.getUniqueId());
        if (loser instanceof Player p) TagManager.recordLoss(p);
        else if (loser != null) TagManager.recordLoss(loser.getUniqueId());

        FileLog.write("combat.log", "MATCH END winner=" + name(winner) + " loser=" + name(loser) + " reason=" + reason);

        ArenaBlockListener.restoreAll();
        MatchManager.endMatch();
        VoteManager.reset();
        cleanup(winner);
        cleanup(loser);
        BotManager.refreshAll();

        if (winner != null) {
            String winnerName = name(winner);
            Bukkit.broadcast(Component.text("§6[Tournament] §f" + winnerName + " §ewon the match!"));
            Title title = Title.title(
                    Component.text("§6§l" + winnerName),
                    Component.text("§ewins the match!"),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }
        TournamentManager.notifyNext();
    }

    /** Ends the match with no winner and no tag changes. */
    public static void draw(String message) {
        cancelTasks();
        LivingEntity[] f = entities();
        for (LivingEntity e : f) tell(e, "§e" + message);
        FileLog.write("combat.log", "MATCH END draw: " + message);

        ArenaBlockListener.restoreAll();
        MatchManager.endMatch();
        VoteManager.reset();
        cleanup(f[0]);
        cleanup(f[1]);
        BotManager.refreshAll();
        TournamentManager.notifyNext();
    }

    private static void cleanup(LivingEntity e) {
        if (e == null) return;
        if (e instanceof Player p) {
            if (!p.isOnline()) return;
            Kits.clearPlayer(p);
            if (!p.isDead()) {
                resetFighter(p);
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(p.getWorld().getSpawnLocation());
            }
        } else if (BotManager.isBot(e.getUniqueId()) && !e.isDead()) {
            Kits.clearBot(e);
            BotAi.reset(e.getUniqueId());
            resetFighter(e);
            Location home = BotManager.home(e.getUniqueId());
            if (home != null) e.teleport(home);
        }
    }

    public static void cancelTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (limitTask != null) {
            limitTask.cancel();
            limitTask = null;
        }
    }

    /** The two fighters as entities (players or bots); null where a fighter is gone. */
    private static LivingEntity[] entities() {
        UUID a = MatchManager.getPlayer1();
        UUID b = MatchManager.getPlayer2();
        return new LivingEntity[]{a != null ? BotManager.get(a) : null, b != null ? BotManager.get(b) : null};
    }

    /** Only the human fighters (bots are null here). */
    private static Player[] fighters() {
        UUID a = MatchManager.getPlayer1();
        UUID b = MatchManager.getPlayer2();
        return new Player[]{a != null ? Bukkit.getPlayer(a) : null, b != null ? Bukkit.getPlayer(b) : null};
    }

    private static void titleToFighters(String text) {
        Title title = Title.title(Component.text(text), Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100)));
        for (Player p : fighters()) {
            if (p != null) p.showTitle(title);
        }
    }

    private static void tell(LivingEntity e, String message) {
        if (e instanceof Player p) p.sendMessage(message);
    }

    private static String name(LivingEntity e) {
        return e != null ? BotManager.nameOf(e) : "none";
    }
}
