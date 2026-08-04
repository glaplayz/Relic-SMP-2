package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicProfile;
import com.relicbearers.relic.RelicType;
import io.papermc.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;

/**
 * Relic items are tied to server-side ownership, not the physical item, so
 * they should never be droppable, and should never be lost on death - only a
 * killer claiming the relic (RelicManager#awardRelicFromKill) can take it
 * from you. They also should never trigger their base material's own vanilla
 * action (sapling/cobblestone placing, ender pearl throwing) - that's
 * blocked here as a second, more reliable layer on top of AbilityListener's
 * PlayerInteractEvent cancellation, since block placement and projectile
 * launches go through their own dedicated events regardless of how the
 * original interaction was categorized.
 */
public class RelicProtectionListener implements Listener {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;

    public RelicProtectionListener(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (relicManager.getRelicTypeFromItem(event.getItemDrop().getItemStack()).isPresent()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You can't drop your relic.");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (relicManager.getRelicTypeFromItem(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED +
                    "That's your relic, not a block - sneak + right-click to use its ability instead.");
        }
    }

    @EventHandler
    public void onLaunchProjectile(PlayerLaunchProjectileEvent event) {
        if (relicManager.getRelicTypeFromItem(event.getItemStack()).isPresent()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED +
                    "That's your relic, not a throwable - sneak + right-click to use its ability instead.");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Strip relic items out of the death drops entirely - the player keeps
        // ownership (tracked in their profile, untouched here) and gets the
        // physical item back on respawn. Only a killer claiming the relic via
        // RelicManager#awardRelicFromKill can take a relic from someone.
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            if (relicManager.getRelicTypeFromItem(it.next()).isPresent()) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Respawn wipes the offhand slot along with the rest of the inventory
        // (unless keepInventory is on) - re-equip the player's currently
        // active relic a tick later, once their inventory is settled.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            RelicProfile profile = relicManager.getProfile(player);
            RelicType active = profile.getPrimaryRelic();
            if (active != null) {
                relicManager.giveRelicItem(player, active);
            }
        });
    }
}
