package com.imposter.game;

import com.imposter.ImposterPlugin;
import com.imposter.gui.VotingGUI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.*;

/**
 * Represents one emergency meeting from the moment it is called until the
 * ejection result is applied.
 */
public class Meeting {

    private final ImposterPlugin plugin;
    private final GameManager gameManager;
    private final UUID calledBy;

    // Snapshot of who is eligible to vote / be voted for.
    private final List<UUID> participants = new ArrayList<>();

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();
    private final Map<UUID, Location> savedLocations = new HashMap<>();
    private final Map<UUID, UUID> votes = new HashMap<>(); // voter -> candidate (null-key handled separately)
    private final Set<UUID> skipped = new HashSet<>();

    private BukkitTask discussionTask;
    private BukkitTask votingTimeoutTask;
    private boolean concluded = false;

    public Meeting(ImposterPlugin plugin, GameManager gameManager, UUID calledBy) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.calledBy = calledBy;
    }

    public void begin() {
        participants.addAll(gameManager.getAlivePlayers());
        Location votingRoom = gameManager.getVotingRoomLocation();

        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            savedLocations.put(id, p.getLocation());
            savedInventories.put(id, p.getInventory().getContents());
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.teleport(votingRoom);
            Player caller = Bukkit.getPlayer(calledBy);
            String callerName = caller != null ? caller.getName() : "Someone";
            p.showTitle(Title.title(
                    net.kyori.adventure.text.Component.text("\u00A7c\u00A7lEMERGENCY MEETING"),
                    net.kyori.adventure.text.Component.text("\u00A77Called by " + callerName),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }

        int discussionSeconds = plugin.getConfig().getInt("meeting-discussion-seconds", 15);
        discussionTask = new BukkitRunnable() {
            int remaining = discussionSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    openVoting();
                    cancel();
                    return;
                }
                for (UUID id : participants) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.sendActionBar(net.kyori.adventure.text.Component.text(
                                "\u00A7eVoting opens in " + remaining + "s..."));
                    }
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void openVoting() {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            VotingGUI.open(this, p, participants);
        }

        int votingSeconds = plugin.getConfig().getInt("meeting-voting-seconds", 20);
        votingTimeoutTask = Bukkit.getScheduler().runTaskLater(plugin, this::conclude, votingSeconds * 20L);
    }

    /** Called by the GUI click listener when a player casts a vote. */
    public void castVote(UUID voter, UUID candidateOrNullForSkip) {
        if (concluded) return;
        if (!participants.contains(voter)) return;
        if (candidateOrNullForSkip == null) {
            skipped.add(voter);
        } else {
            votes.put(voter, candidateOrNullForSkip);
        }
        Player p = Bukkit.getPlayer(voter);
        if (p != null) p.closeInventory();

        if (votes.size() + skipped.size() >= participants.size()) {
            conclude();
        }
    }

    public synchronized void conclude() {
        if (concluded) return;
        concluded = true;
        if (discussionTask != null) discussionTask.cancel();
        if (votingTimeoutTask != null) votingTimeoutTask.cancel();

        // Tally
        Map<UUID, Integer> tally = new HashMap<>();
        for (UUID candidate : votes.values()) {
            tally.merge(candidate, 1, Integer::sum);
        }

        UUID ejected = null;
        int highest = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> e : tally.entrySet()) {
            if (e.getValue() > highest) {
                highest = e.getValue();
                ejected = e.getKey();
                tie = false;
            } else if (e.getValue() == highest) {
                tie = true;
            }
        }
        if (tie || highest == 0) ejected = null; // skip / no majority

        // Restore inventories & positions
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            ItemStack[] items = savedInventories.get(id);
            if (items != null) p.getInventory().setContents(items);
            Location loc = savedLocations.get(id);
            if (loc != null) p.teleport(loc);
            p.setGameMode(GameMode.SURVIVAL);
            p.closeInventory();
        }

        gameManager.onMeetingConcluded(this, ejected);
    }

    public List<UUID> getParticipants() {
        return participants;
    }
}
