package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /queue — anyone: shows the current fight and the upcoming ones.
 * OP only: /queue add <p1> <p2>, /queue insert <#> <p1> <p2>, /queue remove <#>, /queue clear.
 */
public class QueueCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            showQueue(sender);
            return true;
        }

        if (!isEditor(sender)) {
            sender.sendMessage("§cYou don't have permission to change the queue. Use §f/queue §cto view it.");
            return true;
        }

        String[] headBefore = TournamentManager.peek();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length != 3) {
                    sender.sendMessage("§cUsage: /queue add <player1> <player2>");
                    return true;
                }
                if (!validPair(sender, args[1], args[2])) return true;
                TournamentManager.add(args[1], args[2]);
                sender.sendMessage("§aAdded fight #" + TournamentManager.list().size() + ": §f" + args[1] + " §avs §f" + args[2]);
            }
            case "insert" -> {
                if (args.length != 4 || parse(args[1]) < 1) {
                    sender.sendMessage("§cUsage: /queue insert <position> <player1> <player2>");
                    return true;
                }
                if (!validPair(sender, args[2], args[3])) return true;
                TournamentManager.insert(parse(args[1]), args[2], args[3]);
                sender.sendMessage("§aInserted: §f" + args[2] + " §avs §f" + args[3] + " §aat position " + args[1] + ".");
            }
            case "remove" -> {
                if (args.length != 2 || parse(args[1]) < 1) {
                    sender.sendMessage("§cUsage: /queue remove <position>");
                    return true;
                }
                String[] removed = TournamentManager.remove(parse(args[1]));
                if (removed == null) {
                    sender.sendMessage("§cThere is no fight at that position.");
                } else {
                    sender.sendMessage("§aRemoved: §f" + removed[0] + " §avs §f" + removed[1]);
                }
            }
            case "clear" -> {
                TournamentManager.clear();
                sender.sendMessage("§aThe queue was cleared.");
            }
            default -> {
                sender.sendMessage("§cUsage: /queue [add <p1> <p2> | insert <#> <p1> <p2> | remove <#> | clear]");
                return true;
            }
        }

        String[] headAfter = TournamentManager.peek();
        if (headAfter != null && (headBefore == null
                || !headBefore[0].equalsIgnoreCase(headAfter[0]) || !headBefore[1].equalsIgnoreCase(headAfter[1]))) {
            TournamentManager.notifyNext();
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!isEditor(sender)) return out;

        if (args.length == 1) {
            out.addAll(List.of("add", "insert", "remove", "clear", "list"));
        } else {
            String sub = args[0].toLowerCase();
            boolean namePosition = (sub.equals("add") && args.length <= 3)
                    || (sub.equals("insert") && (args.length == 3 || args.length == 4));
            if (namePosition) {
                for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
                out.addAll(BotManager.names());
            } else if (sub.equals("remove") && args.length == 2) {
                for (int i = 1; i <= TournamentManager.list().size(); i++) out.add(String.valueOf(i));
            }
        }
        String prefix = args[args.length - 1].toLowerCase();
        out.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return out;
    }

    private boolean isEditor(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("arena.bracket");
    }

    private void showQueue(CommandSender sender) {
        sender.sendMessage("§6§l=== Fight queue ===");

        if (MatchManager.isMatchActive()) {
            sender.sendMessage("§cNow fighting: " + describe(name(MatchManager.getPlayer1()), name(MatchManager.getPlayer2())));
        } else {
            sender.sendMessage("§7No match in progress.");
        }

        List<String[]> pairs = TournamentManager.list();
        if (pairs.isEmpty()) {
            sender.sendMessage("§7No upcoming fights.");
            return;
        }
        sender.sendMessage("§aNext: " + describe(pairs.get(0)[0], pairs.get(0)[1]));
        for (int i = 1; i < pairs.size(); i++) {
            sender.sendMessage("§e#" + (i + 1) + ": " + describe(pairs.get(i)[0], pairs.get(i)[1]));
        }
    }

    private boolean validPair(CommandSender sender, String a, String b) {
        if (a.equalsIgnoreCase(b)) {
            sender.sendMessage("§cA player can't fight themselves.");
            return false;
        }
        for (String n : new String[]{a, b}) {
            if (TournamentManager.contains(n)) {
                sender.sendMessage("§c" + n + " is already in the queue.");
                return false;
            }
        }
        return true;
    }

    private String describe(String a, String b) {
        return "§f" + a + tag(a) + " §fvs §f" + b + tag(b);
    }

    private String tag(String name) {
        LivingEntity e = BotManager.find(name);
        return e != null ? " §7[" + TagManager.getTagText(e.getUniqueId()) + "]" : "";
    }

    private String name(UUID id) {
        LivingEntity e = id != null ? BotManager.get(id) : null;
        return e != null ? BotManager.nameOf(e) : "?";
    }

    private int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
