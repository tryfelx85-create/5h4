package com.arena.spawn;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Melee AI for bots. Only active while a bot is fighting in a match (after FIGHT!):
 * it turns toward the opponent, walks up to them (jumping over one-block steps),
 * swings on a cooldown with the damage of its weapon, and uses "healing" from its
 * kit (potions / golden apples) when low. Outside a match, and with the level OFF,
 * bots stay frozen dummies. Getting hit briefly hands control back to the physics
 * so knockback works.
 */
public class BotAi implements Listener {

    public enum Level {
        OFF(0, 0, 0, 0, 0, 0, false),
        EASY(20, 2.6, 0.16, 0.35, 6, 80, false),
        NORMAL(14, 3.0, 0.21, 0.15, 8, 50, false),
        HARD(10, 3.2, 0.26, 0.03, 9, 30, true);

        final int attackCooldown;
        final double reach;
        final double speed;
        final double missChance;
        final double healBelowHp;
        final int healCooldown;
        final boolean strafe;

        Level(int attackCooldown, double reach, double speed, double missChance,
              double healBelowHp, int healCooldown, boolean strafe) {
            this.attackCooldown = attackCooldown;
            this.reach = reach;
            this.speed = speed;
            this.missChance = missChance;
            this.healBelowHp = healBelowHp;
            this.healCooldown = healCooldown;
            this.strafe = strafe;
        }
    }

    private static class State {
        int healsLeft;
        long nextAttackTick;
        long nextHealTick;
        long noControlUntilMs;
        long nextStrafeSwitchTick;
        int strafeDir = 1;
        boolean immovable = true;
    }

    private static Level level = Level.NORMAL;
    private static long tick;
    private static final Map<UUID, State> states = new HashMap<>();

    public static void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, BotAi::tick, 1L, 1L);
    }

    public static Level getLevel() {
        return level;
    }

    public static boolean setLevel(String name) {
        try {
            level = Level.valueOf(name.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Called when a bot is dressed for a match: decides how many heals its kit gives it. */
    public static void setKit(UUID id, int kit) {
        State s = states.computeIfAbsent(id, k -> new State());
        s.healsLeft = switch (kit) {
            case 3 -> 6;
            case 4 -> 4;
            case 5 -> 8;
            default -> 0;
        };
    }

    public static void reset(UUID id) {
        states.remove(id);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        UUID id = e.getEntity().getUniqueId();
        if (BotManager.isBot(id)) {
            states.computeIfAbsent(id, k -> new State()).noControlUntilMs = System.currentTimeMillis() + 350;
        }
    }

    private static void tick() {
        tick++;
        for (UUID id : BotManager.ids()) {
            LivingEntity bot = BotManager.get(id);
            if (bot == null || bot.isDead()) continue;

            boolean active = level != Level.OFF
                    && MatchManager.isFighting(id)
                    && MatchManager.isFightStarted()
                    && !MatchManager.isPaused()
                    && !MatchManager.isFrozen();

            State s = states.computeIfAbsent(id, k -> new State());
            bot.setInvulnerable(!MatchManager.isFighting(id)); // lobby bots cannot be hurt
            if (s.immovable == active) {
                s.immovable = !active;
                BotManager.setImmovable(bot, s.immovable);
            }
            if (!active) {
                keepStill(bot, id);
                continue;
            }

            UUID p1 = MatchManager.getPlayer1();
            UUID p2 = MatchManager.getPlayer2();
            UUID otherId = id.equals(p1) ? p2 : p1;
            LivingEntity target = otherId != null ? BotManager.get(otherId) : null;
            if (target == null || target.isDead() || !target.getWorld().equals(bot.getWorld())) continue;

            act(bot, target, s);
        }
    }

    /** Outside a fight a bot never walks: no sideways motion, and it is put back at its spot if it was moved. */
    private static void keepStill(LivingEntity bot, UUID id) {
        Vector v = bot.getVelocity();
        if (v.getX() != 0 || v.getZ() != 0) {
            v.setX(0);
            v.setZ(0);
            bot.setVelocity(v);
        }
        Location home = BotManager.home(id);
        if (home != null && !MatchManager.isFighting(id) && home.getWorld().equals(bot.getWorld())
                && bot.getLocation().distanceSquared(home) > 2.25) {
            bot.teleport(home);
        }
    }

    private static void act(LivingEntity bot, LivingEntity target, State s) {
        Location bl = bot.getLocation();
        Location tl = target.getLocation();
        Vector flat = tl.toVector().subtract(bl.toVector());
        flat.setY(0);
        double dist = flat.length();
        if (dist < 0.01) return;
        Vector dir = flat.clone().normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ()));
        double dy = target.getEyeLocation().getY() - bot.getEyeLocation().getY();
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        bot.setRotation(yaw, pitch);

        if (System.currentTimeMillis() >= s.noControlUntilMs) {
            Vector v = bot.getVelocity();
            double hx = 0, hz = 0;
            if (dist > 2.0) {
                hx = dir.getX() * level.speed;
                hz = dir.getZ() * level.speed;
            }
            if (level.strafe && dist < 4.0) {
                if (tick >= s.nextStrafeSwitchTick) {
                    s.strafeDir = -s.strafeDir;
                    s.nextStrafeSwitchTick = tick + 12 + ThreadLocalRandom.current().nextInt(14);
                }
                hx += -dir.getZ() * level.speed * 0.6 * s.strafeDir;
                hz += dir.getX() * level.speed * 0.6 * s.strafeDir;
            }
            v.setX(hx);
            v.setZ(hz);
            if (bot.isOnGround() && blockedAhead(bl, dir)) {
                v.setY(0.42);
            }
            bot.setVelocity(v);
        }

        if (tick >= s.nextAttackTick && bl.distance(tl) <= level.reach) {
            s.nextAttackTick = tick + level.attackCooldown;
            bot.swingMainHand();
            if (ThreadLocalRandom.current().nextDouble() >= level.missChance) {
                ItemStack weapon = bot.getEquipment() != null ? bot.getEquipment().getItemInMainHand() : null;
                target.damage(damageOf(weapon), bot);
                if (weapon != null && weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT) > 0) {
                    target.setFireTicks(80 * weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT));
                }
            }
        }

        if (s.healsLeft > 0 && tick >= s.nextHealTick && bot.getHealth() <= level.healBelowHp) {
            s.healsLeft--;
            s.nextHealTick = tick + level.healCooldown;
            heal(bot, weaponKit(bot));
        }
    }

    private static boolean blockedAhead(Location bl, Vector dir) {
        Location front = bl.clone().add(dir.clone().multiply(0.7));
        return !front.getBlock().isPassable() && front.clone().add(0, 1, 0).getBlock().isPassable()
                && front.clone().add(0, 2, 0).getBlock().isPassable();
    }

    private static double damageOf(ItemStack weapon) {
        if (weapon == null) return 1.0;
        double base = switch (weapon.getType()) {
            case NETHERITE_SWORD -> 8.0;
            case DIAMOND_SWORD -> 7.0;
            default -> 1.0;
        };
        int sharp = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
        return sharp > 0 ? base + 0.5 * sharp + 0.5 : base;
    }

    /** Netherite gear = kit 5 (potions), enchanted diamond gear = kit 4 (apples), otherwise splash-healing kit 3. */
    private static int weaponKit(LivingEntity bot) {
        if (bot.getEquipment() == null) return 3;
        ItemStack chest = bot.getEquipment().getChestplate();
        if (chest != null && chest.getType().name().startsWith("NETHERITE_")) return 5;
        if (chest != null && chest.getEnchantmentLevel(Enchantment.PROTECTION) > 0) return 4;
        return 3;
    }

    private static void heal(LivingEntity bot, int kit) {
        if (kit == 4) {
            bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            bot.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
        } else {
            bot.setHealth(Math.min(bot.getMaxHealth(), bot.getHealth() + 8.0));
        }
    }
}
