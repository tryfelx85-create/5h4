package com.arena.spawn;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** Values loaded from config.yml (defaults below are used if a key is missing). */
public class Settings {

    public static String ownerNick = "TryFX";

    public static int buttonX = 38, buttonY = 10, buttonZ = 60;
    public static double[] fighter1 = {70, 5, 33};
    public static double[] fighter2 = {70, 5, 90};
    public static double[] lobby = {32, 10, 66};

    public static int voteSeconds = 20;
    public static int countdownSeconds = 3;
    public static int matchTimeLimit = 180;
    public static List<String> blockedCommands = List.of("spectate", "stopspectating", "tp", "teleport");

    public static double alertThreshold = 6.0;
    public static long alertCooldownMs = 5000;
    public static double reachBuffer = 0.5;
    public static double speedMaxBps = 8.0;
    public static double flyMaxRise = 1.5;
    public static double antiKbMinPush = 0.15;
    public static double aimbotPerfectError = 1.5;
    public static double aimbotSnapRotation = 90;
    public static double killauraMaxAngle = 90;

    public static void load(FileConfiguration c) {
        ownerNick = c.getString("owner-nick", ownerNick);

        buttonX = c.getInt("arena.start-button.x", buttonX);
        buttonY = c.getInt("arena.start-button.y", buttonY);
        buttonZ = c.getInt("arena.start-button.z", buttonZ);
        fighter1 = xyz(c, "arena.fighter1", fighter1);
        fighter2 = xyz(c, "arena.fighter2", fighter2);
        lobby = xyz(c, "arena.lobby", lobby);

        voteSeconds = c.getInt("match.kit-vote-seconds", voteSeconds);
        countdownSeconds = c.getInt("match.countdown-seconds", countdownSeconds);
        matchTimeLimit = c.getInt("match.time-limit-seconds", matchTimeLimit);
        if (c.isList("match.blocked-commands")) {
            blockedCommands = c.getStringList("match.blocked-commands").stream().map(String::toLowerCase).toList();
        }

        alertThreshold = c.getDouble("anticheat.alert-threshold", alertThreshold);
        alertCooldownMs = (long) (c.getDouble("anticheat.alert-cooldown-seconds", alertCooldownMs / 1000.0) * 1000);
        reachBuffer = c.getDouble("anticheat.reach-buffer", reachBuffer);
        speedMaxBps = c.getDouble("anticheat.speed-max-bps", speedMaxBps);
        flyMaxRise = c.getDouble("anticheat.fly-max-rise", flyMaxRise);
        antiKbMinPush = c.getDouble("anticheat.antiknockback-min-push", antiKbMinPush);
        aimbotPerfectError = c.getDouble("anticheat.aimbot-perfect-error-degrees", aimbotPerfectError);
        aimbotSnapRotation = c.getDouble("anticheat.aimbot-snap-degrees", aimbotSnapRotation);
        killauraMaxAngle = c.getDouble("anticheat.killaura-max-angle-degrees", killauraMaxAngle);
    }

    private static double[] xyz(FileConfiguration c, String path, double[] def) {
        return new double[]{
                c.getDouble(path + ".x", def[0]),
                c.getDouble(path + ".y", def[1]),
                c.getDouble(path + ".z", def[2])};
    }

    public static Location at(World world, double[] p) {
        return new Location(world, p[0], p[1], p[2]);
    }
}
