package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class AbilityListener implements Listener {

    private final RelicManager relicManager;

    public AbilityListener(RelicManager relicManager) {
        this.relicManager = relicManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Only trigger once per interaction (offhand click fires this event too; filter to main hand)
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        Optional<RelicType> typeOpt = relicManager.getRelicTypeFromItem(offhand);
        if (typeOpt.isEmpty()) return;

        // Require sneaking to activate, so normal right-click interactions (doors, chests) aren't hijacked
        if (!player.isSneaking()) return;

        RelicType type = typeOpt.get();
        String errorMessage = relicManager.tryActivate(player, type);
        if (errorMessage != null) {
            player.sendMessage(errorMessage);
        }
        event.setCancelled(true);
    }
}
