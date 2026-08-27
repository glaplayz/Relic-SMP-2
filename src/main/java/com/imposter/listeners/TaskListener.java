package com.imposter.listeners;

import com.imposter.ImposterPlugin;
import com.imposter.game.GameState;
import com.imposter.game.Role;
import com.imposter.util.ItemUtil;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.equipment.EquipmentSlot;

public class TaskListener implements Listener {

    private final ImposterPlugin plugin;

    public TaskListener(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getRightClicked().getType() != EntityType.VILLAGER) return;

        Player player = event.getPlayer();
        if (plugin.getGameManager().getState() != GameState.RUNNING) return;
        if (!plugin.getGameManager().isInGame(player.getUniqueId())) return;
        if (plugin.getGameManager().getRole(player.getUniqueId()) == Role.IMPOSTER) return; // imposters can't do the task
        if (!plugin.getGameManager().isAlive(player.getUniqueId())) return;

        World.Environment env = player.getWorld().getEnvironment();
        if (env != World.Environment.THE_END) return;

        PlayerInventory inv = player.getInventory();
        ItemStack handItem = inv.getItemInMainHand();
        if (!ItemUtil.isValidTaskItem(handItem)) return;

        Villager villager = (Villager) event.getRightClicked();
        EntityEquipment equipment = villager.getEquipment();
        if (equipment == null) return;

        event.setCancelled(true);

        ItemStack toEquip = handItem.clone();
        toEquip.setAmount(1);
        equipment.setLeggings(toEquip);
        equipment.setLeggingsDropChance(0f);

        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            inv.setItemInMainHand(null);
        }

        player.sendMessage("\u00A7bYou equipped the villager with the required gear!");
        plugin.getGameManager().handleTaskCompleted(player);
    }
}
