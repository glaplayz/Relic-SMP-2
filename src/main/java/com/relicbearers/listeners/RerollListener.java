package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicProfile;
import com.relicbearers.relic.RelicType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * Reroll "recipe" - checked directly against the crafting grid contents
 * (rather than registered as a formal Bukkit Recipe with RecipeChoice
 * matching, which is unreliable for items carrying custom persistent data).
 * Combine exactly 1 relic item with the configured catalyst
 * (reroll.catalyst-material / reroll.catalyst-amount) and nothing else,
 * anywhere in the crafting grid, in any arrangement.
 */
public class RerollListener implements Listener {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;

    public RerollListener(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    private Material catalystMaterial() {
        try {
            return Material.valueOf(plugin.getConfig().getString("reroll.catalyst-material", "ENDER_EYE"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid reroll.catalyst-material in config, defaulting to ENDER_EYE.");
            return Material.ENDER_EYE;
        }
    }

    private int catalystAmount() {
        return Math.max(1, plugin.getConfig().getInt("reroll.catalyst-amount", 3));
    }

    /** Grid must contain exactly 1 relic item + exactly the catalyst amount, and nothing else. */
    private Optional<RelicType> matchGrid(ItemStack[] matrix) {
        RelicType foundRelic = null;
        int catalystCount = 0;
        Material catalyst = catalystMaterial();

        for (ItemStack stack : matrix) {
            if (stack == null || stack.getType() == Material.AIR) continue;

            Optional<RelicType> relicType = relicManager.getRelicTypeFromItem(stack);
            if (relicType.isPresent()) {
                if (foundRelic != null || stack.getAmount() != 1) return Optional.empty(); // only 1 relic item allowed
                foundRelic = relicType.get();
                continue;
            }

            if (stack.getType() == catalyst) {
                catalystCount += stack.getAmount();
                continue;
            }

            return Optional.empty(); // any other item in the grid invalidates the match
        }

        if (foundRelic == null || catalystCount != catalystAmount()) return Optional.empty();
        return Optional.of(foundRelic);
    }

    private ItemStack placeholderItem() {
        ItemStack placeholder = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "??? Rerolled Relic");
        meta.setLore(List.of(
                ChatColor.GRAY + "Take this to reveal your new relic.",
                ChatColor.DARK_GRAY + "Your old relic's progress will be lost."
        ));
        placeholder.setItemMeta(meta);
        return placeholder;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        if (matchGrid(inv.getMatrix()).isPresent()) {
            inv.setResult(placeholderItem());
        }
        // no match -> leave the result slot as whatever vanilla/other plugins computed (normally empty)
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player player)) return;

        CraftingInventory inv = event.getInventory();
        Optional<RelicType> match = matchGrid(inv.getMatrix());
        if (match.isEmpty()) return; // not our recipe, let vanilla/other plugins handle it

        // Keep this a deliberate one-at-a-time craft: no shift-click batching.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Reroll one relic at a time - shift-click is disabled for this recipe.");
            return;
        }

        RelicType oldType = match.get();
        RelicProfile profile = relicManager.getProfile(player);
        if (!profile.owns(oldType)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't own that relic, so you can't reroll it.");
            return;
        }

        RelicType newType = relicManager.rerollRelic(player, oldType);
        if (newType == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack newItem = relicManager.createRelicItem(newType);
        event.setCurrentItem(newItem);

        if (newType == oldType) {
            player.sendMessage(ChatColor.YELLOW + "You already own every other relic type - the reroll had nowhere new to go.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Your " + oldType.getDisplayName() +
                    " relic rerolled into " + newType.coloredName() + ChatColor.GREEN + "! (Tier 1)");
        }
    }
}
