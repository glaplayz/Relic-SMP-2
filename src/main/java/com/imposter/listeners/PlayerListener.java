package com.imposter.listeners;

import com.imposter.ImposterPlugin;
import com.imposter.game.GameState;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ImposterPlugin plugin;

    public PlayerListener(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Global chat disable. Paper fires its native, Adventure-based chat event
     * (AsyncChatEvent) for normal chat; we cancel that. We also cancel the legacy
     * Bukkit AsyncPlayerChatEvent in case any other plugin re-routes through it.
     * Cancelling at MONITOR with ignoreCancelled=false means it's always cancelled
     * last, regardless of what other plugins do to the event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPaperChat(AsyncChatEvent event) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("\u00A7cChat is disabled.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
    }

    /**
     * Blocks the vanilla "say to everyone" style chat commands some servers/plugins
     * expose (e.g. /say, /me, /tell-broadcast variants) so they can't be used to
     * route around the chat disable. Adjust this list if it's too aggressive for
     * your server's other plugins.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/say ") || msg.equals("/say") || msg.startsWith("/me ")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("\u00A7cChat is disabled.");
        }
    }

    /** Always suppress the vanilla death message/broadcast. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (plugin.getGameManager().getState() != GameState.RUNNING) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!plugin.getGameManager().isInGame(victim.getUniqueId())) return;
        if (!plugin.getGameManager().isInGame(damager.getUniqueId())) return;

        // Only the imposter's hits actually eliminate crewmates; make the
        // killing blow lethal but silent (no death message/broadcast).
        double finalDamage = event.getFinalDamage();
        if (victim.getHealth() - finalDamage <= 0) {
            event.setCancelled(true);
            victim.setHealth(0.0);
            plugin.getGameManager().handleKill(victim, damager);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        plugin.getGameManager().leave(event.getPlayer());
    }
}
