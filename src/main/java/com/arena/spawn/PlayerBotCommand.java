package com.arena.spawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.List;

/**
 * OP only: /playerbot add | remove [name] | list.
 * Bots are numbered (Bot1, Bot2, ...), have a tag, and can be used in /queue and /tag like players.
 */
public class PlayerBotCommand implements TabExecutor {

    private static final String USAGE = "§cUsage: /playerbot <add | remove [name] | list | ai [off|easy|normal|hard]>";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!allowed(sender)) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(USAGE);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }
                Entity bot = BotManager.spawn(p.getLocation());
                String name = BotManager.nameOf((org.bukkit.entity.LivingEntity) bot);
                sender.sendMessage("§a" + name + " spawned. Use the name in §f/queue add §aand §f/tag§a.");
                if (bot instanceof Zombie) {
                    sender.sendMessage("§eThis server version has no player-model entity (needs Paper 1.21.9+), so a frozen zombie was spawned instead.");
                }
            }
            case "remove" -> {
                if (args.length >= 2) {
                    sender.sendMessage(BotManager.remove(args[1]) ? "§aRemoved " + args[1] + "." : "§cNo bot with that name.");
                } else {
                    sender.sendMessage("§aRemoved " + BotManager.removeAll() + " bot(s).");
                }
            }
            case "list" -> {
                List<String> names = BotManager.names();
                sender.sendMessage(names.isEmpty() ? "§7No bots." : "§6Bots: §f" + String.join(", ", names));
            }
            case "ai" -> {
                if (args.length < 2) {
                    sender.sendMessage("§6Bot AI level: §f" + BotAi.getLevel().name().toLowerCase());
                } else if (BotAi.setLevel(args[1])) {
                    sender.sendMessage("§aBot AI level set to §f" + BotAi.getLevel().name().toLowerCase() + "§a.");
                } else {
                    sender.sendMessage(USAGE);
                }
            }
            default -> sender.sendMessage(USAGE);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!allowed(sender)) return out;
        if (args.length == 1) {
            out.addAll(List.of("add", "remove", "list", "ai"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            out.addAll(BotManager.names());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ai")) {
            out.addAll(List.of("off", "easy", "normal", "hard"));
        }
        String prefix = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return out;
    }

    private boolean allowed(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("arena.bracket");
    }
}
