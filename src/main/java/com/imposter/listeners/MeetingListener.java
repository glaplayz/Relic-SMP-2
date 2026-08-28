package com.imposter.listeners;

import com.imposter.ImposterPlugin;
import com.imposter.util.ItemUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class MeetingListener implements Listener {

    private final ImposterPlugin plugin;

    public MeetingListener(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!ItemUtil.isMeetingBell(plugin, item)) return;

        event.setCancelled(true);
        boolean used = plugin.getGameManager().useBell(event.getPlayer());
        if (used) {
            // Consume one bell from the stack.
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                event.getPlayer().getInventory().remove(item);
            }
        } else {
            event.getPlayer().sendMessage("\u00A7cYou can't call a meeting right now.");
        }
    }
}
