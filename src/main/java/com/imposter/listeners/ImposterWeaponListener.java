package com.imposter.listeners;

import com.imposter.ImposterPlugin;
import com.imposter.game.GameManager;
import com.imposter.game.GameState;
import com.imposter.game.Role;
import com.imposter.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the Imposter's "Silent Blade" - a guaranteed one-shot kill on any
 * crewmate, gated by a cooldown (default 60s, configurable). This intentionally
 * bypasses normal damage/health math so eliminations aren't skill-based combat;
 * ordinary weapons/fists never kill (see PlayerListener.onDamage, which clamps
 * regular damage so it can never actually finish someone off).
 */
public class ImposterWeaponListener implements Listener {

    private final ImposterPlugin plugin;

    public ImposterWeaponListener(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAttack(EntityDamageByEntityEvent event) {
        GameManager gm = plugin.getGameManager();
        if (gm.getState() != GameState.RUNNING) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!gm.isInGame(victim.getUniqueId()) || !gm.isInGame(damager.getUniqueId())) return;
        if (gm.getRole(damager.getUniqueId()) != Role.IMPOSTER) return;
        if (!gm.isAlive(victim.getUniqueId())) return;

        ItemStack hand = damager.getInventory().getItemInMainHand();
        if (!ItemUtil.isImposterWeapon(plugin, hand)) return; // not the special weapon - let normal (non-lethal) damage apply

        // We always take over damage resolution for the Silent Blade rather
        // than letting vanilla math decide the outcome.
        event.setCancelled(true);
        gm.markAttacked(victim.getUniqueId());

        int remaining = gm.getWeaponCooldownRemaining(damager.getUniqueId());
        if (remaining > 0) {
            damager.sendActionBar(Component.text("\u00A7cSilent Blade on cooldown: " + remaining + "s"));
            return;
        }

        // Guaranteed, instant elimination regardless of the victim's current health.
        victim.setHealth(0.0);
        gm.startWeaponCooldown(damager.getUniqueId());
        gm.handleKill(victim, damager);
        damager.sendActionBar(Component.text("\u00A74You silently eliminated " + victim.getName() + "!"));
    }
}
