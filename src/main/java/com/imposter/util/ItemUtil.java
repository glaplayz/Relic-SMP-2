package com.imposter.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class ItemUtil {

    public static final String BELL_KEY = "imposter_meeting_bell";
    public static final String WEAPON_KEY = "imposter_kill_weapon";

    /**
     * Checks whether the given item satisfies the shared crewmate task:
     * Diamond Leggings, Netherite armor trim, Swift Sneak III.
     */
    public static boolean isValidTaskItem(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_LEGGINGS) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Swift Sneak III check
        if (!meta.hasEnchant(Enchantment.SWIFT_SNEAK)) return false;
        if (meta.getEnchantLevel(Enchantment.SWIFT_SNEAK) < 3) return false;

        // Netherite trim check
        if (!(meta instanceof ArmorMeta)) return false;
        ArmorMeta armorMeta = (ArmorMeta) meta;
        if (!armorMeta.hasTrim()) return false;
        ArmorTrim trim = armorMeta.getTrim();
        return trim.getMaterial().equals(TrimMaterial.NETHERITE);
    }

    /**
     * Builds a "reference" copy of the task item (used for /imposter give-task-item,
     * so admins/testers can spawn a correctly-enchanted item quickly).
     */
    public static ItemStack buildReferenceTaskItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
        if (meta instanceof ArmorMeta armorMeta) {
            ArmorTrim trim = new ArmorTrim(TrimMaterial.NETHERITE, TrimPattern.VEX);
            armorMeta.setTrim(trim);
            item.setItemMeta(armorMeta);
        } else {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack buildMeetingBell(Plugin plugin) {
        ItemStack bell = new ItemStack(Material.BELL);
        ItemMeta meta = bell.getItemMeta();
        meta.setDisplayName("\u00A76\u00A7lEmergency Meeting Bell");
        meta.setLore(java.util.List.of(
                "\u00A77Right-click to call an emergency meeting.",
                "\u00A77You may only use this once per game."
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, BELL_KEY), PersistentDataType.BYTE, (byte) 1);
        bell.setItemMeta(meta);
        return bell;
    }

    public static boolean isMeetingBell(Plugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.BELL || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, BELL_KEY), PersistentDataType.BYTE);
    }

    /**
     * Builds the Imposter's guaranteed one-shot kill weapon. A single hit on a
     * crewmate eliminates them instantly regardless of their current health -
     * balanced by a long cooldown (enforced in code, not by this item alone).
     */
    public static ItemStack buildImposterWeapon(Plugin plugin, int cooldownSeconds) {
        ItemStack weapon = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("\u00A74\u00A7l\u2620 Silent Blade");
        meta.setLore(java.util.List.of(
                "\u00A77One hit eliminates a crewmate instantly.",
                "\u00A77Cooldown: " + cooldownSeconds + " seconds between kills.",
                "\u00A78Attacking on cooldown does nothing."
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, WEAPON_KEY), PersistentDataType.BYTE, (byte) 1);
        weapon.setItemMeta(meta);
        return weapon;
    }

    public static boolean isImposterWeapon(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, WEAPON_KEY), PersistentDataType.BYTE);
    }
}
