package com.relicbearers.relic;

import org.bukkit.Material;
import org.bukkit.ChatColor;

/**
 * The 8 relic types. Each has a themed relic item (worn/held in offhand)
 * and a themed color used in chat and item names.
 */
public enum RelicType {

    EMBER("Ember", Material.MAGMA_CREAM, ChatColor.RED,
            "Fire & forge. Fire Nova ability, fire immunity passive."),
    TIDE("Tide", Material.HEART_OF_THE_SEA, ChatColor.AQUA,
            "Water & flow. Riptide Wave ability, water breathing passive."),
    GROWTH("Growth", Material.OAK_SAPLING, ChatColor.GREEN,
            "Nature & life. Bloom Burst ability, faster regen passive."),
    GUST("Gust", Material.FEATHER, ChatColor.WHITE,
            "Air & motion. Wind Dash ability, reduced fall damage passive."),
    STONE("Stone", Material.COBBLESTONE, ChatColor.GRAY,
            "Earth & defense. Bulwark ability, damage reduction passive."),
    GLIMMER("Glimmer", Material.GLOWSTONE_DUST, ChatColor.YELLOW,
            "Light & sight. Flashbang ability, night vision passive."),
    VOID("Void", Material.ENDER_PEARL, ChatColor.DARK_PURPLE,
            "Shadow & space. Blink ability, blindness immunity passive."),
    AURUM("Aurum", Material.RAW_GOLD, ChatColor.GOLD,
            "Gold & fortune. Midas Touch ability, bonus PvE XP passive.");

    private final String displayName;
    private final Material itemMaterial;
    private final ChatColor color;
    private final String description;

    RelicType(String displayName, Material itemMaterial, ChatColor color, String description) {
        this.displayName = displayName;
        this.itemMaterial = itemMaterial;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public String coloredName() {
        return color + displayName + ChatColor.RESET;
    }
}
