package com.imposter.gui;

import com.imposter.game.Meeting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public class VotingGUI {

    public static final int SKIP_SLOT = 49;

    /**
     * Builds and opens a voting inventory for a single voter, listing all alive
     * candidates (including the voter, since the imposter is unknown) as player
     * heads, plus a "Skip Vote" button.
     */
    public static void open(Meeting meeting, Player voter, List<UUID> candidates) {
        VotingInventoryHolder holder = new VotingInventoryHolder(meeting, voter.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, 54, "\u00A74\u00A7lCast Your Vote");
        holder.setInventory(inv);

        int slot = 0;
        for (UUID candidate : candidates) {
            if (slot >= 45) break; // reserve bottom row
            OfflinePlayer op = Bukkit.getOfflinePlayer(candidate);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            String name = op.getName() != null ? op.getName() : candidate.toString();
            meta.setDisplayName("\u00A7e" + name);
            meta.setLore(List.of("\u00A77Click to vote to eject this player."));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
        }

        ItemStack skip = new ItemStack(Material.BARRIER);
        ItemMeta skipMeta = skip.getItemMeta();
        skipMeta.setDisplayName("\u00A7cSkip Vote");
        skipMeta.setLore(List.of("\u00A77Cast an abstain / skip vote."));
        skip.setItemMeta(skipMeta);
        inv.setItem(SKIP_SLOT, skip);

        voter.openInventory(inv);
    }
}
