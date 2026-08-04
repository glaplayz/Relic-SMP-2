package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AbilityListener implements Listener {

    private final RelicManager relicManager;

    // Depending on what's in the player's main hand, a single physical right
    // click can fire PlayerInteractEvent for MAIN_HAND, OFF_HAND, or both -
    // which hand actually gets the event is decided client-side and isn't
    // reliable to predict. This tracks the last server tick we already
    // activated an ability on, per player, so we never double-activate if
    // both fire, while still activating no matter which one does.
    private final Map<UUID, Long> lastActivationTick = new HashMap<>();

    public AbilityListener(RelicManager relicManager) {
        this.relicManager = relicManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        Optional<RelicType> typeOpt = relicManager.getRelicTypeFromItem(offhand);
        if (typeOpt.isEmpty()) return; // not holding a relic in offhand at all

        // The relic item must never perform its base-material vanilla action
        // (throwing an ender pearl, placing a sapling/cobblestone, etc.).
        // That vanilla action only ever comes from the off-hand item being
        // the one actually used, so this is the case to shut down here.
        // (Block placement and projectile launches also get a second,
        // more reliable layer of protection in RelicProtectionListener.)
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        }

        // Require sneaking to activate, so normal main-hand interactions
        // (doors, chests, tools) still work when the player isn't trying
        // to use their relic.
        if (!player.isSneaking()) return;

        // Sneak + right-click always means "use my relic," regardless of
        // whether this particular event is the MAIN_HAND or OFF_HAND one -
        // cancel it either way so nothing in the main hand (food, blocks,
        // etc.) also triggers at the same time.
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        long tick = player.getWorld().getFullTime();
        Long lastTick = lastActivationTick.get(player.getUniqueId());
        if (lastTick != null && lastTick == tick) {
            return; // already activated once for this exact click this tick
        }
        lastActivationTick.put(player.getUniqueId(), tick);

        RelicType type = typeOpt.get();
        String errorMessage = relicManager.tryActivate(player, type);
        if (errorMessage != null) {
            player.sendMessage(errorMessage);
        }
    }
}
