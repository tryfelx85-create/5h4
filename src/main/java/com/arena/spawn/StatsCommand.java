package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /top — leaderboard by wins. /stats [player] — one player's record. */
public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("top")) {
            showTop(sender);
        } else {
            showStats(sender, args);
        }
        return true;
    }

    private void showTop(CommandSender sender) {
        List<Map.Entry<UUID, TagManager.PlayerTag>> entries = new ArrayList<>();
        for (Map.Entry<UUID, TagManager.PlayerTag> entry : TagManager.all().entrySet()) {
            if (!entry.getValue().isModers()) entries.add(entry);
        }
        entries.sort((a, b) -> Integer.compare(b.getValue().wins, a.getValue().wins));

        sender.sendMessage("§6§l=== Top players ===");
        if (entries.isEmpty()) {
            sender.sendMessage("§7No results yet.");
            return;
        }
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            TagManager.PlayerTag t = entries.get(i).getValue();
            String name = Bukkit.getOfflinePlayer(entries.get(i).getKey()).getName();
            sender.sendMessage("§e#" + (i + 1) + " §f" + (name != null ? name : "?")
                    + " §7- §a" + t.wins + " wins §7/ §c" + t.losses + " losses");
        }
    }

    private void showStats(CommandSender sender, String[] args) {
        String name = args.length > 0 ? args[0] : sender.getName();
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        TagManager.PlayerTag tag = TagManager.getTag(target.getUniqueId());
        if (tag == null) {
            sender.sendMessage("§cNo data for that player.");
            return;
        }
        sender.sendMessage("§6§l=== " + name + " ===");
        sender.sendMessage("§7Tag: " + TagManager.colorFor(tag) + tag.text);
        sender.sendMessage("§7Wins: §a" + tag.wins + " §7Losses: §c" + tag.losses);
    }
}
