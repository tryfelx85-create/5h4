package com.arena.spawn;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class Kits {

    public static void clearPlayer(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
        inv.setItemInOffHand(null);
    }

    private static void giveDiamondArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inv.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inv.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    // Kit 1: Diamond armor + diamond sword + 6 steaks
    public static void giveKit1(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();
        inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
        inv.addItem(new ItemStack(Material.COOKED_BEEF, 6));
    }

    // Kit 2: Diamond armor + diamond sword + diamond axe + shield + 6 steaks
    public static void giveKit2(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();
        inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
        inv.addItem(new ItemStack(Material.DIAMOND_AXE));
        inv.setItemInOffHand(new ItemStack(Material.SHIELD));
        inv.addItem(new ItemStack(Material.COOKED_BEEF, 6));
    }

    // Kit 3: Diamond armor + diamond sword + inventory full of Splash Potion of Healing II
    public static void giveKit3(Player player) {
        clearPlayer(player);
        giveDiamondArmor(player);
        PlayerInventory inv = player.getInventory();

        // Give the sword first and remember its slot so the fill loop doesn't overwrite it
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        inv.addItem(sword);

        ItemStack healingPotion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) healingPotion.getItemMeta();
        meta.setBasePotionType(PotionType.STRONG_HEALING); // Splash Potion of Healing II
        healingPotion.setItemMeta(meta);

        // FIX: fill ALL main-inventory slots (0-35), not just 1-35, so slot 0
        // isn't silently left empty. Since giveDiamondArmor/addItem only ever
        // occupy armor slots + wherever the sword landed, this now correctly
        // fills every remaining empty slot including slot 0.
        for (int slot = 0; slot < 36; slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, healingPotion.clone());
            }
        }
    }

    private static ItemStack enchanted(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        item.addEnchantment(enchantment, level);
        return item;
    }

    // Kit 4 (UHC): Protection I diamond armor, Sharpness II sword, Power II bow + 32 arrows,
    // 8 golden apples, 2 lava + 2 water buckets, 8 cobwebs, 2 stacks of cobblestone,
    // Efficiency II diamond pickaxe. Fighters are switched to Survival so they can place blocks.
    public static void giveKit4(Player player) {
        clearPlayer(player);
        player.setGameMode(GameMode.SURVIVAL);

        PlayerInventory inv = player.getInventory();
        inv.setHelmet(enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 1));
        inv.setChestplate(enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 1));
        inv.setLeggings(enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 1));
        inv.setBoots(enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 1));

        inv.setItem(0, enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2));
        inv.setItem(1, enchanted(Material.BOW, Enchantment.POWER, 2));
        inv.setItem(2, new ItemStack(Material.GOLDEN_APPLE, 8));
        inv.setItem(3, new ItemStack(Material.COBBLESTONE, 64));
        inv.setItem(4, enchanted(Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY, 2));
        inv.setItem(5, new ItemStack(Material.WATER_BUCKET));
        inv.setItem(6, new ItemStack(Material.LAVA_BUCKET));
        inv.setItem(7, new ItemStack(Material.COBWEB, 8));

        inv.addItem(new ItemStack(Material.WATER_BUCKET));
        inv.addItem(new ItemStack(Material.LAVA_BUCKET));
        inv.addItem(new ItemStack(Material.COBBLESTONE, 64));
        inv.addItem(new ItemStack(Material.ARROW, 32));
    }

    private static ItemStack potion(Material type, PotionType potionType) {
        ItemStack item = new ItemStack(type);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(potionType);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack netheriteArmor(Material material, boolean boots) {
        ItemStack item = new ItemStack(material);
        item.addEnchantment(Enchantment.PROTECTION, 4);
        item.addEnchantment(Enchantment.UNBREAKING, 3);
        if (boots) item.addEnchantment(Enchantment.FEATHER_FALLING, 4);
        return item;
    }

    // Kit 5 (Nether Pot): full Protection IV netherite, Sharpness V + Fire Aspect II netherite sword,
    // totem in the off hand, golden apples, ender pearls, Speed II / Strength II / Fire Resistance
    // potions and every remaining slot filled with Splash Instant Health II.
    public static void giveKit5(Player player) {
        clearPlayer(player);
        PlayerInventory inv = player.getInventory();

        inv.setHelmet(netheriteArmor(Material.NETHERITE_HELMET, false));
        inv.setChestplate(netheriteArmor(Material.NETHERITE_CHESTPLATE, false));
        inv.setLeggings(netheriteArmor(Material.NETHERITE_LEGGINGS, false));
        inv.setBoots(netheriteArmor(Material.NETHERITE_BOOTS, true));

        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        sword.addEnchantment(Enchantment.SHARPNESS, 5);
        sword.addEnchantment(Enchantment.UNBREAKING, 3);
        sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        inv.setItem(0, sword);

        inv.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        inv.setItem(1, new ItemStack(Material.ENDER_PEARL, 16));
        inv.setItem(2, new ItemStack(Material.GOLDEN_APPLE, 16));
        inv.setItem(3, potion(Material.POTION, PotionType.STRONG_SWIFTNESS));
        inv.setItem(4, potion(Material.POTION, PotionType.STRONG_STRENGTH));
        inv.setItem(5, potion(Material.POTION, PotionType.LONG_FIRE_RESISTANCE));

        ItemStack healing = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
        for (int slot = 0; slot < 36; slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, healing.clone());
            }
        }
    }

    /** Dresses a bot to match the chosen kit: same armor and weapon (and off-hand item) as a player would get. */
    public static void equipBot(LivingEntity bot, int kit) {
        EntityEquipment eq = bot.getEquipment();
        if (eq == null) return;
        BotAi.setKit(bot.getUniqueId(), kit);

        if (kit == 5) {
            eq.setHelmet(netheriteArmor(Material.NETHERITE_HELMET, false));
            eq.setChestplate(netheriteArmor(Material.NETHERITE_CHESTPLATE, false));
            eq.setLeggings(netheriteArmor(Material.NETHERITE_LEGGINGS, false));
            eq.setBoots(netheriteArmor(Material.NETHERITE_BOOTS, true));
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addEnchantment(Enchantment.SHARPNESS, 5);
            sword.addEnchantment(Enchantment.UNBREAKING, 3);
            sword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            eq.setItemInMainHand(sword);
            eq.setItemInOffHand(new ItemStack(Material.TOTEM_OF_UNDYING));
        } else if (kit == 4) {
            eq.setHelmet(enchanted(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 1));
            eq.setChestplate(enchanted(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 1));
            eq.setLeggings(enchanted(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 1));
            eq.setBoots(enchanted(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 1));
            eq.setItemInMainHand(enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2));
        } else {
            eq.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            eq.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            eq.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            eq.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
            eq.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            if (kit == 2) eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        }

        // Drop chances only exist for Mobs (the zombie fallback); a Mannequin throws here. Drops are cleared on death anyway.
        if (bot instanceof org.bukkit.entity.Mob) {
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
            eq.setItemInOffHandDropChance(0f);
        }
    }

    public static void clearBot(LivingEntity bot) {
        EntityEquipment eq = bot.getEquipment();
        if (eq != null) eq.clear();
    }

    public static void giveKitByChoice(Player player, int kitChoice) {
        switch (kitChoice) {
            case 1 -> giveKit1(player);
            case 2 -> giveKit2(player);
            case 3 -> giveKit3(player);
            case 4 -> giveKit4(player);
            case 5 -> giveKit5(player);
        }
    }
}
