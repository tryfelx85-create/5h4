package com.arena.spawn;

import com.arena.anticheat.AimbotCheck;
import com.arena.anticheat.AntiKnockbackCheck;
import com.arena.anticheat.CheckUtil;
import com.arena.anticheat.FlyCheck;
import com.arena.anticheat.KillauraCheck;
import com.arena.anticheat.ReachCheck;
import com.arena.anticheat.SpeedCheck;
import com.arena.anticheat.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ArenaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Settings.load(getConfig());
        FileLog.init(getDataFolder());
        MatchFlow.init(this);
        TournamentManager.init(this);

        getLogger().info("ArenaPlugin has been enabled!");

        getCommand("spectate").setExecutor(new SpectateCommand());
        getCommand("stopspectating").setExecutor(new StopSpectatingCommand());
        getCommand("tag").setExecutor(new TagCommand());
        getCommand("queue").setExecutor(new QueueCommand());
        getCommand("playerbot").setExecutor(new PlayerBotCommand());
        StatsCommand stats = new StatsCommand();
        getCommand("top").setExecutor(stats);
        getCommand("stats").setExecutor(stats);
        MatchAdminCommand matchAdmin = new MatchAdminCommand();
        getCommand("endmatch").setExecutor(matchAdmin);
        getCommand("pausematch").setExecutor(matchAdmin);

        // Tag system: "0 Wins" / "N Wins" / "Defeated" / custom labels shown
        // beside each player's nickname, loaded/saved from tags.yml.
        TagManager.init(this);
        getServer().getPluginManager().registerEvents(new TagListener(), this);

        // FIX: StartButtonListener now needs the plugin instance to start the vote timer
        StartButtonListener startButton = new StartButtonListener(this);
        getServer().getPluginManager().registerEvents(startButton, this);
        getCommand("agree").setExecutor(new AgreeCommand(startButton));
        getServer().getPluginManager().registerEvents(new PvPRestrictionListener(), this);
        getServer().getPluginManager().registerEvents(new FightRestrictionListener(), this);
        getServer().getPluginManager().registerEvents(new CombatLogListener(), this);
        getServer().getPluginManager().registerEvents(new ArenaBlockListener(), this);
        getServer().getPluginManager().registerEvents(new BotListener(), this);
        getServer().getPluginManager().registerEvents(new BotAi(), this);
        BotAi.start(this);
        getServer().getPluginManager().registerEvents(new VoteListener(), this);
        getServer().getPluginManager().registerEvents(new MatchEndListener(), this); // FIX: new — closes match-end gap

        // Anti-cheat checks
        ViolationManager.init(getLogger());
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ViolationManager(), this);
        pm.registerEvents(new CheckUtil(), this);
        pm.registerEvents(new ReachCheck(), this);
        pm.registerEvents(new SpeedCheck(), this);
        pm.registerEvents(new FlyCheck(), this);
        pm.registerEvents(new AntiKnockbackCheck(this), this);
        pm.registerEvents(new AimbotCheck(), this);
        pm.registerEvents(new KillauraCheck(), this);

        startDecayTask();

        // FIX: infinite saturation + regeneration for anyone not currently
        // fighting and not spectating, removed automatically once they enter
        // a match or start spectating.
        IdleEffectsManager.start(this);

        FightHud.start(this);
    }

    /** Periodically decays violation scores so old flags don't accumulate forever. */
    private void startDecayTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (ViolationManager.CheckType type : ViolationManager.CheckType.values()) {
                    ViolationManager.decay(player, type, 0.5);
                }
            }
        }, 20L * 10, 20L * 10); // every 10 seconds
    }

    @Override
    public void onDisable() {
        BotManager.removeAll();
        TagManager.save();
        ArenaBlockListener.restoreAll();
        FightHud.remove();
        FightHud.clearSuffixes();
        getLogger().info("ArenaPlugin has been disabled!");
    }
}
