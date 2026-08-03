package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicProfile;
import com.relicbearers.relic.RelicType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

/**
 * Registers a crafting-table recipe that rerolls a relic item you're holding
 * into a different random relic type. Combine the relic item with the
 * configured catalyst (reroll.catalyst-material / reroll.catalyst-amount).
 *
 * The result slot always shows a "???" placeholder - the new type is only
 * decided and applied when the item is actually taken out of the result slot.
 */
public class RerollListener implements Listener {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;
    private final NamespacedKey recipeKey;

    public RerollListener(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
        this.recipeKey = new NamespacedKey(plugin, "relic_reroll");
    }

    public void registerRecipe() {
        Material catalystMaterial;
        try {
            catalystMaterial = Material.valueOf(plugin.getConfig().getString("reroll.catalyst-material", "ENDER_EYE"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid reroll.catalyst-material in config, defaulting to ENDER_EYE.");
            catalystMaterial = Material.ENDER_EYE;
        }
        int catalystAmount = Math.max(1, plugin.getConfig().getInt("reroll.catalyst-amount", 3));

        List<ItemStack> relicTemplates = List.of(RelicType.values()).stream()
                .map(relicManager::createRelicItem)
                .toList();

        ItemStack placeholder = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "??? Rerolled Relic");
        meta.setLore(List.of(
                ChatColor.GRAY + "Take this to reveal your new relic.",
                ChatColor.DARK_GRAY + "Your old relic's progress will be lost."
        ));
        placeholder.setItemMeta(meta);

        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, placeholder);
        recipe.addIngredient(1, new RecipeChoice.ExactChoice(relicTemplates));
        recipe.addIngredient(catalystAmount, new RecipeChoice.MaterialChoice(catalystMaterial));

        plugin.getServer().removeRecipe(recipeKey); // safe if this is a /reload
        plugin.getServer().addRecipe(recipe);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe() == null || !(event.getRecipe() instanceof org.bukkit.Keyed keyed)
                || !keyed.getKey().equals(recipeKey)) {
            return;
        }

        HumanEntity human = event.getWhoClicked();
        if (!(human instanceof Player player)) return;

        // Keep this a deliberate one-at-a-time craft: no shift-click batching.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Reroll one relic at a time - shift-click is disabled for this recipe.");
            return;
        }

        Optional<RelicType> oldType = findRelicInMatrix(event.getInventory().getMatrix());
        if (oldType.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        RelicProfile profile = relicManager.getProfile(player);
        if (!profile.owns(oldType.get())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't own that relic, so you can't reroll it.");
            return;
        }

        RelicType newType = relicManager.rerollRelic(player, oldType.get());
        if (newType == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack newItem = relicManager.createRelicItem(newType);
        event.setCurrentItem(newItem);

        if (newType == oldType.get()) {
            player.sendMessage(ChatColor.YELLOW + "You already own every other relic type - the reroll had nowhere new to go.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Your " + oldType.get().getDisplayName() +
                    " relic rerolled into " + newType.coloredName() + ChatColor.GREEN + "! (Tier 1)");
        }
    }

    private Optional<RelicType> findRelicInMatrix(ItemStack[] matrix) {
        for (ItemStack stack : matrix) {
            Optional<RelicType> type = relicManager.getRelicTypeFromItem(stack);
            if (type.isPresent()) return type;
        }
        return Optional.empty();
    }
}
