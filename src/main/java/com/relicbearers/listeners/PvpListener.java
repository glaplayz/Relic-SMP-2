package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PvP is normal server-wide (toggle with global-pvp-enabled). Killing another
 * player is the only way to gain a relic type you don't already own - see
 * RelicManager#awardRelicFromKill. Death-drop protection and respawn
 * restoration for the victim's own relic live in RelicProtectionListener.
 */
public class PvpListener implements Listener {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;

    public PvpListener(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvpDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player damager = resolvePlayerDamager(event);
        if (damager == null) return;

        boolean globalPvp = plugin.getConfig().getBoolean("global-pvp-enabled", true);
        if (!globalPvp) {
            event.setCancelled(true);
            damager.sendMessage(ChatColor.RED + "PvP is currently disabled on this server.");
        }
    }

    private Player resolvePlayerDamager(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        // arrows / tridents thrown by players
        if (event.getDamager() instanceof org.bukkit.entity.Projectile proj
                && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        relicManager.awardRelicFromKill(killer, victim);
    }
}
