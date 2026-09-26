package com.arena.spawn;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class VoteManager {

    // Voting now lasts 20 seconds (was 30)

    // player UUID -> kit choice (1, 2, or 3)
    private static final Map<UUID, Integer> votes = new HashMap<>();
    private static final Random random = new Random();
    private static BukkitTask countdownTask;
    private static boolean resolved = false;

    public static void reset() {
        votes.clear();
        resolved = false;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    // FIX: replaced the single 30s delayed task with a repeating 1-second
    // countdown task, so players actually see the timer (via action bar)
    // instead of it silently running in the background. Fires onTimeUp()
    // once it reaches 0.
    public static void startTimer(ArenaPlugin plugin) {
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int secondsLeft = Settings.voteSeconds;

            @Override
            public void run() {
                if (resolved) {
                    if (countdownTask != null) {
                        countdownTask.cancel();
                        countdownTask = null;
                    }
                    return;
                }

                if (secondsLeft <= 0) {
                    if (countdownTask != null) {
                        countdownTask.cancel();
                        countdownTask = null;
                    }
                    onTimeUp();
                    return;
                }

                sendCountdown(secondsLeft);
                secondsLeft--;
            }
        }, 0L, 20L); // runs every second, starting immediately
    }

    private static void sendCountdown(int secondsLeft) {
        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        Component message = Component.text("§eChoose your kit: §f" + secondsLeft + "s remaining");

        Player player1 = p1 != null ? Bukkit.getPlayer(p1) : null;
        Player player2 = p2 != null ? Bukkit.getPlayer(p2) : null;
        if (player1 != null) player1.sendActionBar(message);
        if (player2 != null) player2.sendActionBar(message);
    }

    public static void castVote(Player player, int kitChoice) {
        if (resolved) return;

        // FIX: reject invalid kit choices instead of silently accepting them
        if (kitChoice < 1 || kitChoice > 5) {
            player.sendMessage("§cInvalid kit choice.");
            return;
        }

        votes.put(player.getUniqueId(), kitChoice);

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();

        // A bot opponent always "votes" for whatever its human opponent picked
        if (p1 != null && BotManager.isBot(p1)) votes.put(p1, kitChoice);
        if (p2 != null && BotManager.isBot(p2)) votes.put(p2, kitChoice);

        if (votes.containsKey(p1) && votes.containsKey(p2)) {
            int vote1 = votes.get(p1);
            int vote2 = votes.get(p2);

            // Only resolve early if both agree; if they differ, wait for the timer
            // and then randomly pick between their two choices.
            if (vote1 == vote2) {
                resolveVote(p1, p2, vote1, false);
            } else {
                Player player1 = Bukkit.getPlayer(p1);
                Player player2 = Bukkit.getPlayer(p2);
                if (player1 != null) player1.sendMessage("§7Votes differ — waiting for the timer to decide...");
                if (player2 != null) player2.sendMessage("§7Votes differ — waiting for the timer to decide...");
            }
        }
    }

    private static void onTimeUp() {
        if (resolved) return;

        UUID p1 = MatchManager.getPlayer1();
        UUID p2 = MatchManager.getPlayer2();
        if (p1 == null || p2 == null) return;

        // Default anyone who didn't vote to Kit 1
        int vote1 = votes.getOrDefault(p1, 1);
        int vote2 = votes.getOrDefault(p2, 1);

        int finalKit = (vote1 == vote2) ? vote1 : (random.nextBoolean() ? vote1 : vote2);
        resolveVote(p1, p2, finalKit, vote1 != vote2);
    }

    private static void resolveVote(UUID p1, UUID p2, int finalKit, boolean wasRandomTiebreak) {
        resolved = true;
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        Player player1 = Bukkit.getPlayer(p1);
        Player player2 = Bukkit.getPlayer(p2);

        // FIX: the vote GUI stays open while voting so players can change their
        // mind (see VoteListener) — now that the vote is actually resolved, close
        // it for both players before handing out the kit.
        if (player1 != null) player1.closeInventory();
        if (player2 != null) player2.closeInventory();

        if (player1 != null) Kits.giveKitByChoice(player1, finalKit);
        if (player2 != null) Kits.giveKitByChoice(player2, finalKit);

        for (UUID id : new UUID[]{p1, p2}) {
            if (id != null && BotManager.isBot(id)) {
                org.bukkit.entity.LivingEntity bot = BotManager.get(id);
                try {
                    if (bot != null) Kits.equipBot(bot, finalKit);
                } catch (RuntimeException ex) {
                    Bukkit.getLogger().warning("[ArenaPlugin] Could not equip bot: " + ex);
                }
            }
        }

        String kitName = switch (finalKit) {
            case 1 -> "Sword & Steaks";
            case 2 -> "Sword, Axe & Shield";
            case 3 -> "Healing Potions";
            case 4 -> "UHC";
            case 5 -> "Nether Pot";
            default -> "Unknown";
        };

        String resultMessage = wasRandomTiebreak
                ? "§eTime's up! Votes differed — kit chosen at random: §f" + kitName
                : "§aBoth players chose the same kit: §f" + kitName;

        for (Player p : new Player[]{player1, player2}) {
            if (p != null) p.sendMessage(resultMessage);
        }

        votes.clear();
        MatchFlow.beginCountdown();
    }
}
