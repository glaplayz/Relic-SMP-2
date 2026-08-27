package com.imposter.gui;

import com.imposter.game.Meeting;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Marks an Inventory as belonging to a specific player's voting screen during a meeting.
 */
public class VotingInventoryHolder implements InventoryHolder {

    private final Meeting meeting;
    private final UUID voter;
    private Inventory inventory;

    public VotingInventoryHolder(Meeting meeting, UUID voter) {
        this.meeting = meeting;
        this.voter = voter;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public UUID getVoter() {
        return voter;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
