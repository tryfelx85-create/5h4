package com.arena.spawn;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * The start button picks the next fight from the queue, but the fight only
 * begins after the owner confirms it with /agree (see AgreeCommand).
 * Fighters can be players or bots.
 */
public class StartButtonListener implements Listener {

    private final ArenaPlugin plugin;

    /** Fight waiting for the owner's /agree: names as they are in the queue. */
    private String[] pendingNames;

    public StartButtonListener(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onButtonPress(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (!event.getClickedBlock().getType().name().endsWith("_BUTTON")) return;

        // Only react to the designated start button, not every button on the map
        if (event.getClickedBlock().getX() != Settings.buttonX
                || event.getClickedBlock().getY() != Settings.buttonY
                || event.getClickedBlock().getZ() != Settings.buttonZ) {
            return;
        }

        Player presser = event.getPlayer();

        if (MatchManager.isMatchActive()) {
            presser.sendMessage("§cA match is already in progress.");
            return;
        }

        String[] names = TournamentManager.peek();
        if (names == null) {
            presser.sendMessage("§cThe queue is empty. An operator must add a fight with §f/queue add <player1> <player2>§c.");
            return;
        }

        LivingEntity[] fighters = resolve(names, presser);
        if (fighters == null) return;

        Player confirmer = Bukkit.getPlayerExact(Settings.ownerNick);
        if (confirmer == null) {
            presser.sendMessage("§c" + Settings.ownerNick + " must be online to confirm the fight.");
            return;
        }

        pendingNames = names;
        presser.sendMessage("§eWaiting for " + Settings.ownerNick + " to confirm the fight...");
        confirmer.sendMessage("§6[Tournament] §eConfirm fight: §f" + BotManager.nameOf(fighters[0])
                + " §7[" + TagManager.getTagText(fighters[0].getUniqueId()) + "] §evs §f" + BotManager.nameOf(fighters[1])
                + " §7[" + TagManager.getTagText(fighters[1].getUniqueId()) + "]§e. Type §a/agree §eto start.");
    }

    /** Called by /agree. Returns an error message to show, or null if the fight started. */
    public String confirm() {
        if (pendingNames == null) return "There is no fight waiting for confirmation.";
        if (MatchManager.isMatchActive()) return "A match is already in progress.";

        String[] names = pendingNames;
        Player confirmer = Bukkit.getPlayerExact(Settings.ownerNick);
        LivingEntity[] fighters = resolve(names, confirmer);
        if (fighters == null) return null; // resolve() already told the confirmer why

        pendingNames = null;
        String[] head = TournamentManager.peek();
        if (head != null && head[0].equalsIgnoreCase(names[0]) && head[1].equalsIgnoreCase(names[1])) {
            TournamentManager.removeFirst();
        }
        startMatch(fighters[0], fighters[1]);
        return null;
    }

    /** Looks up both fighters (players or bots) and checks they can fight; tells `notify` what is wrong otherwise. */
    private LivingEntity[] resolve(String[] names, Player notify) {
        LivingEntity a = BotManager.find(names[0]);
        LivingEntity b = BotManager.find(names[1]);
        if (a == null || b == null) {
            tell(notify, "§cNext fight can't start, offline: §f"
                    + (a == null ? names[0] + " " : "") + (b == null ? names[1] : ""));
            return null;
        }
        for (LivingEntity e : new LivingEntity[]{a, b}) {
            if (e instanceof Player p && p.getGameMode() != GameMode.ADVENTURE) {
                tell(notify, "§cBoth players of the next fight must be in Adventure mode.");
                return null;
            }
        }
        if (TagManager.isModers(a.getUniqueId()) || TagManager.isModers(b.getUniqueId())) {
            tell(notify, "§cModers can't take part in matches.");
            return null;
        }
        if (TagManager.isDefeated(a.getUniqueId()) || TagManager.isDefeated(b.getUniqueId())) {
            tell(notify, "§cA defeated player can't be matched into a fight again.");
            return null;
        }
        return new LivingEntity[]{a, b};
    }

    private void tell(Player p, String msg) {
        if (p != null) p.sendMessage(msg);
    }

    private void startMatch(LivingEntity first, LivingEntity second) {
        World world = first.getWorld();

        first.teleport(Settings.at(world, Settings.fighter1));
        second.teleport(Settings.at(world, Settings.fighter2));
        MatchFlow.resetFighter(first);
        MatchFlow.resetFighter(second);

        String name1 = BotManager.nameOf(first);
        String name2 = BotManager.nameOf(second);
        FileLog.write("combat.log", "MATCH START " + name1 + " vs " + name2);

        MatchManager.startMatch(first.getUniqueId(), second.getUniqueId());
        VoteManager.reset();
        VoteManager.startTimer(plugin);
        BotManager.refreshAll();

        for (LivingEntity e : new LivingEntity[]{first, second}) {
            if (e instanceof Player p) {
                p.sendMessage("§aMatch started! Choose your kit.");
                VoteGUI.open(p);
            }
        }

        for (Player p : world.getPlayers()) {
            if (!p.equals(first) && !p.equals(second)) {
                p.sendMessage("§eA match has started between §f" + name1 + "§e and §f" + name2 + "§e.");
            }
        }
    }
}
