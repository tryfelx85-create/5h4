package com.arena.spawn;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {

    private static final Set<UUID> activeFighters = ConcurrentHashMap.newKeySet();
    private static volatile UUID player1;
    private static volatile UUID player2;

    /** False during kit voting and the countdown: nobody can be damaged yet. */
    private static volatile boolean fightStarted;
    /** True during the countdown and while an operator has paused the match. */
    private static volatile boolean frozen;
    private static volatile boolean paused;

    public static void startMatch(UUID p1, UUID p2) {
        activeFighters.clear();
        activeFighters.add(p1);
        activeFighters.add(p2);
        player1 = p1;
        player2 = p2;
        fightStarted = false;
        frozen = false;
        paused = false;
    }

    public static void endMatch() {
        activeFighters.clear();
        player1 = null;
        player2 = null;
        fightStarted = false;
        frozen = false;
        paused = false;
    }

    public static boolean isFighting(UUID uuid) {
        return activeFighters.contains(uuid);
    }

    public static boolean isMatchActive() {
        return !activeFighters.isEmpty();
    }

    public static UUID getPlayer1() {
        return player1;
    }

    public static UUID getPlayer2() {
        return player2;
    }

    public static boolean isFightStarted() {
        return fightStarted;
    }

    public static void setFightStarted(boolean value) {
        fightStarted = value;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void setFrozen(boolean value) {
        frozen = value;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean value) {
        paused = value;
    }
}
