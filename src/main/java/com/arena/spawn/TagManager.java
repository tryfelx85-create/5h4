package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Player tags shown beside the nickname (above the head, in tab and in chat).
 *
 * Normal players: the tag is their win count as a bare number ("0", "1", ...),
 * or "Defeated" after a loss. Two normal players can only fight if their tags
 * are identical, and "Defeated" players can never fight again.
 *
 * Master: can be assigned to any player by the bracket; losing to Master counts as a normal loss.
 * Moders: red tag, always OP, and can never take part in a match.
 */
public class TagManager {

    public static final String MASTER = "Master";
    public static final String MODERS = "Moders";
    public static final String DEFEATED = "Defeated";

    private static ArenaPlugin plugin;
    private static File file;
    private static FileConfiguration config;

    private static final Map<UUID, PlayerTag> tags = new HashMap<>();

    public static void init(ArenaPlugin pluginInstance) {
        plugin = pluginInstance;
        file = new File(plugin.getDataFolder(), "tags.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create tags.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private static void load() {
        tags.clear();
        if (config.getConfigurationSection("players") == null) return;

        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            String path = "players." + key + ".";
            int wins = config.getInt(path + "wins", 0);
            boolean defeated = config.getBoolean(path + "defeated", false);
            boolean custom = config.getBoolean(path + "custom", false);
            String text = config.getString(path + "text", String.valueOf(wins));
            if (!custom && !defeated) {
                text = String.valueOf(wins); // migrates old "N Wins" labels to plain numbers
            }
            PlayerTag loaded = new PlayerTag(text, wins, defeated, custom);
            loaded.losses = config.getInt(path + "losses", 0);
            tags.put(uuid, loaded);
        }
    }

    public static void save() {
        if (config == null || file == null) return;

        for (Map.Entry<UUID, PlayerTag> entry : tags.entrySet()) {
            String path = "players." + entry.getKey() + ".";
            PlayerTag t = entry.getValue();
            config.set(path + "text", t.text);
            config.set(path + "wins", t.wins);
            config.set(path + "defeated", t.defeated);
            config.set(path + "custom", t.custom);
            config.set(path + "losses", t.losses);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save tags.yml", e);
        }
    }

    public static void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerTag tag = tags.get(uuid);

        if (tag == null) {
            if (player.getName().equalsIgnoreCase(Settings.ownerNick)) {
                tag = new PlayerTag(MASTER, 0, false, true);
            } else {
                tag = new PlayerTag("0", 0, false, false);
            }
            tags.put(uuid, tag);
            save();
        }

        if (tag.isModers() && !player.isOp()) {
            player.setOp(true);
        }

        applyDisplay(player, tag);
    }

    /** Deletes a tag record entirely (used for removed bots so they never reach tags.yml). */
    public static void forget(UUID uuid) {
        tags.remove(uuid);
        if (config != null) {
            config.set("players." + uuid, null);
        }
    }

    public static Map<UUID, PlayerTag> all() {
        return tags;
    }

    public static PlayerTag getTag(UUID uuid) {
        return tags.get(uuid);
    }

    public static String getTagText(UUID uuid) {
        PlayerTag tag = tags.get(uuid);
        return tag != null ? tag.text : "0";
    }

    public static boolean isDefeated(UUID uuid) {
        PlayerTag tag = tags.get(uuid);
        return tag != null && tag.defeated;
    }

    public static boolean isMaster(UUID uuid) {
        PlayerTag tag = tags.get(uuid);
        return tag != null && tag.isMaster();
    }

    public static boolean isModers(UUID uuid) {
        PlayerTag tag = tags.get(uuid);
        return tag != null && tag.isModers();
    }

    /** Whether two players may be paired. Moders never; Master with anyone; otherwise identical tags. */
    public static boolean canFight(UUID a, UUID b) {
        if (isModers(a) || isModers(b)) return false;
        if (isMaster(a) || isMaster(b)) return true;
        return getTagText(a).equals(getTagText(b));
    }

    /** Winner gets +1 win. Master's/custom tags keep their text. */
    public static void recordWin(Player winner) {
        applyDisplay(winner, recordWin(winner.getUniqueId()));
    }

    /** Same, for any fighter identified by UUID (e.g. a bot). */
    public static PlayerTag recordWin(UUID uuid) {
        PlayerTag tag = getOrCreate(uuid);
        tag.wins++;
        if (!tag.custom) {
            tag.text = String.valueOf(tag.wins);
        }
        save();
        return tag;
    }

    public static PlayerTag getOrCreate(UUID uuid) {
        return tags.computeIfAbsent(uuid, id -> new PlayerTag("0", 0, false, false));
    }

    /** Loser becomes "Defeated" permanently (also when losing to Master), except Master and Moders themselves. */
    public static void recordLoss(Player loser) {
        applyDisplay(loser, recordLoss(loser.getUniqueId()));
    }

    public static PlayerTag recordLoss(UUID uuid) {
        PlayerTag tag = getOrCreate(uuid);
        tag.losses++;
        if (!tag.isMaster() && !tag.isModers()) {
            tag.defeated = true;
            tag.text = DEFEATED;
        }
        save();
        return tag;
    }

    /**
     * OP-only. A plain number sets the win count (and keeps auto-updating).
     * Any other text is a custom tag. "Moders" grants OP; changing away from it removes OP.
     */
    public static void setTag(OfflinePlayer target, String text) {
        boolean wasModers = isModers(target.getUniqueId());
        PlayerTag tag = updateTag(target.getUniqueId(), text);

        if (tag.isModers()) {
            target.setOp(true);
        } else if (wasModers) {
            target.setOp(false);
        }

        Player online = target.getPlayer();
        if (online != null) {
            applyDisplay(online, tag);
        }
    }

    /** Applies tag text to a UUID (no OP or scoreboard side effects) and saves. */
    public static PlayerTag updateTag(UUID uuid, String text) {
        PlayerTag tag = getOrCreate(uuid);

        if (text.matches("\\d{1,9}")) {
            tag.wins = Integer.parseInt(text);
            tag.text = String.valueOf(tag.wins);
            tag.custom = false;
            tag.defeated = false;
        } else if (text.equalsIgnoreCase(MODERS)) {
            tag.text = MODERS;
            tag.custom = true;
            tag.defeated = false;
        } else if (text.equalsIgnoreCase(MASTER)) {
            tag.text = MASTER;
            tag.custom = true;
            tag.defeated = false;
        } else {
            tag.text = text;
            tag.custom = true;
            tag.defeated = text.equalsIgnoreCase(DEFEATED);
        }

        save();
        return tag;
    }

    public static String colorFor(PlayerTag tag) {
        if (tag.isModers()) return "§c§l";
        if (tag.isMaster()) return "§6§l";
        if (tag.defeated) return "§c";
        if (tag.custom) return "§e";
        return "§7";
    }

    /** Text shown right after the nickname (above the head and in tab); pass "" to clear. */
    public static void setSuffix(Player player, String suffix) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("tag_" + player.getUniqueId().toString().substring(0, 12));
        if (team != null) {
            team.setSuffix(suffix);
        }
    }

    private static void applyDisplay(Player player, PlayerTag tag) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "tag_" + player.getUniqueId().toString().substring(0, 12);

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        for (Team t : board.getTeams()) {
            if (t.getName().startsWith("tag_") && !t.getName().equals(teamName) && t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        team.setPrefix(colorFor(tag) + "[" + tag.text + "] §r");
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public static class PlayerTag {
        public String text;
        public int wins;
        public boolean defeated;
        public boolean custom;
        public int losses;

        public PlayerTag(String text, int wins, boolean defeated, boolean custom) {
            this.text = text;
            this.wins = wins;
            this.defeated = defeated;
            this.custom = custom;
        }

        public boolean isMaster() {
            return text.equalsIgnoreCase(MASTER);
        }

        public boolean isModers() {
            return text.equalsIgnoreCase(MODERS);
        }
    }
}
