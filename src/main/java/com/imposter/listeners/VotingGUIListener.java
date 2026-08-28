package com.imposter.listeners;

import com.imposter.gui.VotingGUI;
import com.imposter.gui.VotingInventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class VotingGUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VotingInventoryHolder holder)) return;

        event.setCancelled(true); // never let players take voting-GUI items

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(holder.getVoter())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (event.getSlot() == VotingGUI.SKIP_SLOT) {
            holder.getMeeting().castVote(holder.getVoter(), null);
            return;
        }

        if (clicked.getItemMeta() instanceof SkullMeta skullMeta && skullMeta.getOwningPlayer() != null) {
            holder.getMeeting().castVote(holder.getVoter(), skullMeta.getOwningPlayer().getUniqueId());
        }
    }
}
