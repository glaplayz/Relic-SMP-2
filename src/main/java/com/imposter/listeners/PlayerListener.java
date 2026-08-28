package com.imposter.listeners;

import com.imposter.ImposterPlugin;
import com.imposter.game.GameState;
import com.imposter.game.Role;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final ImposterPlugin plugin;

    public PlayerListener(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    /** Always suppress the vanilla death message/broadcast. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        // Belt-and-suspenders: null the Component-based message (the real source
        // of truth on modern Paper), explicitly tell Paper not to show/broadcast
        // any death message at all, and clear the deprecated String field too in
        // case anything still reads it.
        event.deathMessage(null);
        event.setShowDeathMessages(false);
        event.setDeathMessage(null);
    }

    /**
     * Ordinary combat between game participants never actually kills - only the
     * Imposter's dedicated Silent Blade (handled in ImposterWeaponListener) does.
     * This still marks the victim as "recently attacked" so they can't bell-escape
     * a fight, and clamps any damage that would otherwise be lethal.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (plugin.getGameManager().getState() != GameState.RUNNING) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!plugin.getGameManager().isInGame(victim.getUniqueId())) return;
        if (!plugin.getGameManager().isInGame(damager.getUniqueId())) return;

        if (plugin.getGameManager().getRole(damager.getUniqueId()) == Role.IMPOSTER) {
            plugin.getGameManager().markAttacked(victim.getUniqueId());
        }

        // Never let non-weapon damage finish someone off - avoids a "skill based"
        // fistfight/sword-whittling kill path outside the one-shot weapon.
        double finalDamage = event.getFinalDamage();
        if (victim.getHealth() - finalDamage <= 0) {
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        plugin.getGameManager().leave(event.getPlayer());
    }
}
