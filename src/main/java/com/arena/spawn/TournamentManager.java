package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/** Manually maintained fight queue (pairs of nicknames), saved to queue.yml so it survives restarts. */
public class TournamentManager {

    private static final List<String[]> queue = new ArrayList<>();
    private static ArenaPlugin plugin;
    private static File file;

    public static void init(ArenaPlugin instance) {
        plugin = instance;
        file = new File(plugin.getDataFolder(), "queue.yml");
        queue.clear();
        if (!file.exists()) return;
        for (String line : YamlConfiguration.loadConfiguration(file).getStringList("queue")) {
            String[] pair = line.split("\\|");
            if (pair.length == 2) queue.add(pair);
        }
    }

    private static void save() {
        if (file == null) return;
        List<String> lines = new ArrayList<>();
        for (String[] pair : queue) lines.add(pair[0] + "|" + pair[1]);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("queue", lines);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save queue.yml", e);
        }
    }

    public static void add(String a, String b) {
        queue.add(new String[]{a, b});
        save();
    }

    /** Inserts a pair at a 1-based position (clamped to the queue size). */
    public static void insert(int position, String a, String b) {
        int index = Math.max(0, Math.min(position - 1, queue.size()));
        queue.add(index, new String[]{a, b});
        save();
    }

    /** Removes the fight at a 1-based position; returns it, or null if the position is invalid. */
    public static String[] remove(int position) {
        if (position < 1 || position > queue.size()) return null;
        String[] removed = queue.remove(position - 1);
        save();
        return removed;
    }

    public static void clear() {
        queue.clear();
        save();
    }

    public static List<String[]> list() {
        return new ArrayList<>(queue);
    }

    public static String[] peek() {
        return queue.isEmpty() ? null : queue.get(0);
    }

    public static void removeFirst() {
        if (!queue.isEmpty()) {
            queue.remove(0);
            save();
        }
    }

    public static boolean contains(String name) {
        for (String[] pair : queue) {
            if (pair[0].equalsIgnoreCase(name) || pair[1].equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Tells the two players of the first queued fight that they are up next. */
    public static void notifyNext() {
        String[] next = peek();
        if (next == null) return;
        for (String name : next) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null) {
                p.sendMessage("§a§l[Tournament] §eYou are in the next fight! Get ready.");
            }
        }
    }
}
