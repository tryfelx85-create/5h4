package com.arena.spawn;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class VoteGUI {

    public static final String TITLE = "Choose your kit";

    public static void open(Player player) {
        VoteInventoryHolder holder = new VoteInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text(TITLE));
        holder.setInventory(inv); // FIX: wire the holder to the inventory it owns

        inv.setItem(0, buildIcon(Material.DIAMOND_SWORD, "Kit 1: Sword & Steaks",
                "Diamond armor + diamond sword", "6 cooked steaks"));

        inv.setItem(2, buildIcon(Material.DIAMOND_AXE, "Kit 2: Sword, Axe & Shield",
                "Diamond armor + diamond sword", "Diamond axe + shield", "6 cooked steaks"));

        inv.setItem(4, buildIcon(Material.SPLASH_POTION, "Kit 3: Healing Potions",
                "Diamond armor + diamond sword", "Inventory full of Splash Potion of Healing II"));

        inv.setItem(6, buildIcon(Material.GOLDEN_APPLE, "Kit 4: UHC",
                "Protection I diamond armor", "Sharpness II sword, Power II bow + 32 arrows",
                "8 golden apples, 2 lava + 2 water buckets", "8 cobwebs, 128 cobblestone, Efficiency II pickaxe"));

        inv.setItem(8, buildIcon(Material.NETHERITE_SWORD, "Kit 5: Nether Pot",
                "Protection IV netherite armor", "Sharpness V + Fire Aspect II netherite sword",
                "Totem, 16 golden apples, 16 ender pearls", "Speed II, Strength II, Fire Resistance",
                "Inventory full of Splash Healing II"));

        player.openInventory(inv);
    }

    private static ItemStack buildIcon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.text(line));
        }
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }
}
