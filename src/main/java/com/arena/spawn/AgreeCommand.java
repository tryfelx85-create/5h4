package com.arena.spawn;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/** /agree — only TryFX; confirms the fight that is waiting after the start button was pressed. */
public class AgreeCommand implements CommandExecutor {

    private final StartButtonListener startButton;

    public AgreeCommand(StartButtonListener startButton) {
        this.startButton = startButton;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.getName().equalsIgnoreCase(Settings.ownerNick)) {
            sender.sendMessage("§cOnly " + Settings.ownerNick + " can confirm fights.");
            return true;
        }

        String error = startButton.confirm();
        if (error != null) {
            sender.sendMessage("§c" + error);
        }
        return true;
    }
}
