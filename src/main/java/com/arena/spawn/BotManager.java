package com.arena.spawn;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Numbered practice bots (Bot1, Bot2, ...). Each bot has a tag like a player,
 * can be put in the fight queue by name, and can be a fighter in a match.
 * On Paper 1.21.9+ a bot is a Mannequin (player model); on older versions
 * it falls back to a frozen zombie.
 */
public class BotManager {

    private static class Bot {
        final String name;
        final Location home;

        Bot(String name, Location home) {
            this.name = name;
            this.home = home;
        }
    }

    private static final Map<UUID, Bot> bots = new LinkedHashMap<>();

    public static boolean isBot(UUID id) {
        return bots.containsKey(id);
    }

    /** Any living entity (player or bot) by UUID, or null. */
    public static LivingEntity get(UUID id) {
        Entity e = Bukkit.getEntity(id);
        return e instanceof LivingEntity living ? living : null;
    }

    public static String nameOf(LivingEntity e) {
        Bot bot = bots.get(e.getUniqueId());
        return bot != null ? bot.name : e.getName();
    }

    public static LivingEntity findBot(String name) {
        for (Map.Entry<UUID, Bot> entry : bots.entrySet()) {
            if (entry.getValue().name.equalsIgnoreCase(name)) return get(entry.getKey());
        }
        return null;
    }

    /** A player by exact nickname, otherwise a bot by name. */
    public static LivingEntity find(String name) {
        Player p = Bukkit.getPlayerExact(name);
        return p != null ? p : findBot(name);
    }

    public static Location home(UUID id) {
        Bot bot = bots.get(id);
        return bot != null ? bot.home : null;
    }

    public static List<UUID> ids() {
        return new ArrayList<>(bots.keySet());
    }

    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (Bot b : bots.values()) out.add(b.name);
        return out;
    }

    public static Entity spawn(Location loc) {
        int number = 1;
        while (findBotName("Bot" + number)) number++;
        String name = "Bot" + number;

        EntityType mannequin = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft("mannequin"));
        Entity entity;
        if (mannequin != null) {
            entity = loc.getWorld().spawnEntity(loc, mannequin);
            setImmovable(entity, true);
        } else {
            entity = loc.getWorld().spawn(loc, Zombie.class, z -> {
                z.setAI(false);
                z.setBaby(false);
                z.setShouldBurnInDay(false);
                z.setSilent(true);
            });
        }
        entity.setPersistent(false);
        entity.setInvulnerable(true);
        entity.setCustomNameVisible(true);
        if (entity instanceof LivingEntity living) {
            living.setMaxHealth(20.0);
            living.setHealth(20.0);
        }

        bots.put(entity.getUniqueId(), new Bot(name, loc.clone()));
        TagManager.getOrCreate(entity.getUniqueId());
        refreshName(entity.getUniqueId());
        return entity;
    }

    private static boolean findBotName(String name) {
        for (Bot b : bots.values()) {
            if (b.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Mannequin#setImmovable only exists on new API versions, so it is called reflectively. */
    public static void setImmovable(Entity mannequin, boolean immovable) {
        try {
            Class<?> type = Class.forName("org.bukkit.entity.Mannequin");
            type.getMethod("setImmovable", boolean.class).invoke(mannequin, immovable);
        } catch (ReflectiveOperationException ignored) {
            // zombie fallback (or older API): nothing to toggle
        }
    }

    /** Rebuilds the name above the bot: [tag] BotN (+ current HP while it is fighting). */
    public static void refreshName(UUID id) {
        Bot bot = bots.get(id);
        Entity e = Bukkit.getEntity(id);
        if (bot == null || e == null) return;

        TagManager.PlayerTag tag = TagManager.getOrCreate(id);
        String text = TagManager.colorFor(tag) + "[" + tag.text + "] §f" + bot.name;
        if (MatchManager.isMatchActive() && MatchManager.isFighting(id) && e instanceof LivingEntity living) {
            text += String.format(" §c❤%.1f", Math.max(0, living.getHealth()));
        }
        e.customName(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    public static void refreshAll() {
        for (UUID id : new ArrayList<>(bots.keySet())) refreshName(id);
    }

    /** Drops a bot from the registry together with its tag record (call when the entity is gone). */
    public static void forget(UUID id) {
        bots.remove(id);
        TagManager.forget(id);
    }

    public static boolean remove(String name) {
        for (Map.Entry<UUID, Bot> entry : new ArrayList<>(bots.entrySet())) {
            if (entry.getValue().name.equalsIgnoreCase(name)) {
                Entity e = Bukkit.getEntity(entry.getKey());
                if (e != null) e.remove();
                forget(entry.getKey());
                return true;
            }
        }
        return false;
    }

    public static int removeAll() {
        int count = 0;
        for (UUID id : new ArrayList<>(bots.keySet())) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) {
                e.remove();
                count++;
            }
            forget(id);
        }
        return count;
    }
}
