package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/** OP only: /endmatch (ends the match as a draw) and /pausematch (toggles pause). */
public class MatchAdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("arena.bracket")) {
            sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }
        if (!MatchManager.isMatchActive()) {
            sender.sendMessage("§cThere is no match in progress.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("endmatch")) {
            MatchFlow.draw("The match was ended by an operator.");
            sender.sendMessage("§aMatch ended.");
        } else {
            boolean nowPaused = !MatchManager.isPaused();
            MatchManager.setPaused(nowPaused);
            String msg = nowPaused ? "§e§lThe match is paused." : "§a§lThe match continues!";
            for (UUID id : new UUID[]{MatchManager.getPlayer1(), MatchManager.getPlayer2()}) {
                Player p = id != null ? Bukkit.getPlayer(id) : null;
                if (p != null) p.sendMessage(msg);
            }
            sender.sendMessage(nowPaused ? "§aMatch paused." : "§aMatch resumed.");
        }
        return true;
    }
}
