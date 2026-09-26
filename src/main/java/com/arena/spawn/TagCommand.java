package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/** /tag <player> <text> — OP-only, sets a player's arena tag directly. */
public class TagCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("arena.tag")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /tag <player> <text>");
            return true;
        }

        org.bukkit.entity.LivingEntity bot = BotManager.findBot(args[0]);
        if (bot != null) {
            String botText = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            TagManager.updateTag(bot.getUniqueId(), botText);
            BotManager.refreshName(bot.getUniqueId());
            sender.sendMessage("§aSet §f" + BotManager.nameOf(bot) + "§a's tag to §f\"" + botText + "\"§a.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cThat player has never joined this server.");
            return true;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        TagManager.setTag(target, text);

        sender.sendMessage("§aSet §f" + target.getName() + "§a's tag to §f\"" + text + "\"§a.");
        return true;
    }
}
