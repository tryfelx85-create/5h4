package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Keeps infinite Saturation + Regeneration ("healing") on every player who is
 * NOT currently in a match (not one of the two active fighters) and NOT in
 * Spectator mode. As soon as a player starts a match, or switches to
 * Spectator, the buffs are stripped; they come back automatically once the
 * player is idle again (match ends, or they stop spectating).
 *
 * Implemented as a lightweight repeating task rather than hooking every
 * possible state change (join, respawn, gamemode change, match start/end,
 * etc.) individually — this keeps it simple and self-correcting even if some
 * other part of the plugin changes a player's state without going through
 * MatchManager.
 */
public class IdleEffectsManager {

    // Re-checked every 10 ticks (0.5s) so the buff appears/disappears quickly
    // around match start/end without spamming effect updates every tick.
    private static final long CHECK_PERIOD_TICKS = 10L;

    private static final PotionEffectType[] IDLE_EFFECTS = {
            PotionEffectType.SATURATION,
            PotionEffectType.REGENERATION
    };

    public static void start(JavaPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean shouldHaveBuffs = !MatchManager.isFighting(player.getUniqueId())
                        && player.getGameMode() != GameMode.SPECTATOR;

                if (shouldHaveBuffs) {
                    applyIdleBuffs(player);
                } else {
                    removeIdleBuffs(player);
                }
            }
        }, 0L, CHECK_PERIOD_TICKS);
    }

    private static void applyIdleBuffs(Player player) {
        for (PotionEffectType type : IDLE_EFFECTS) {
            // Only (re)apply if missing, so we're not spamming the same
            // infinite effect onto the player every half-second.
            if (player.getPotionEffect(type) == null) {
                player.addPotionEffect(new PotionEffect(
                        type,
                        PotionEffect.INFINITE_DURATION,
                        0,      // amplifier: level I
                        true,   // ambient (softer particles)
                        false,  // no particles
                        false   // no icon spam
                ));
            }
        }
    }

    private static void removeIdleBuffs(Player player) {
        for (PotionEffectType type : IDLE_EFFECTS) {
            if (player.hasPotionEffect(type)) {
                player.removePotionEffect(type);
            }
        }
    }
}
