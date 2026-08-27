package com.imposter.game;

import com.imposter.ImposterPlugin;
import com.imposter.util.ItemUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.*;

public class GameManager {

    private final ImposterPlugin plugin;

    private GameState state = GameState.WAITING;
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Set<UUID> eliminated = new HashSet<>(); // dead OR ejected
    private final Set<UUID> bellUsed = new HashSet<>();
    private final Set<UUID> lobby = new LinkedHashSet<>();

    private boolean taskCompleted = false;
    private Meeting activeMeeting = null;

    public GameManager(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Lobby management ----------

    public boolean join(Player player) {
        if (state != GameState.WAITING) return false;
        lobby.add(player.getUniqueId());
        return true;
    }

    public boolean leave(Player player) {
        return lobby.remove(player.getUniqueId());
    }

    public Set<UUID> getLobby() {
        return lobby;
    }

    // ---------- Game lifecycle ----------

    /** Starts a game, using the configured default-imposters count. */
    public void start(CommandSender starter) {
        start(starter, plugin.getConfig().getInt("default-imposters", 1));
    }

    /** Starts a game with an explicit number of secret Imposters. */
    public void start(CommandSender starter, int numImposters) {
        int min = plugin.getConfig().getInt("min-players", 3);
        if (lobby.size() < min) {
            starter.sendMessage("\u00A7cNeed at least " + min + " players in the lobby to start (currently " + lobby.size() + ").");
            return;
        }
        if (numImposters < 1) {
            starter.sendMessage("\u00A7cThere must be at least 1 Imposter.");
            return;
        }
        if (numImposters >= lobby.size()) {
            starter.sendMessage("\u00A7cToo many Imposters for " + lobby.size()
                    + " players - there must be at least 1 Crewmate left over.");
            return;
        }

        roles.clear();
        eliminated.clear();
        bellUsed.clear();
        taskCompleted = false;
        activeMeeting = null;

        List<UUID> players = new ArrayList<>(lobby);
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            UUID id = players.get(i);
            roles.put(id, i < numImposters ? Role.IMPOSTER : Role.CREWMATE);
        }

        state = GameState.RUNNING;

        Location spawn = getGameSpawnLocation();
        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(spawn);
            p.getInventory().addItem(ItemUtil.buildMeetingBell(plugin));

            Role role = roles.get(id);
            if (role == Role.IMPOSTER) {
                p.showTitle(Title.title(
                        Component.text("\u00A74\u00A7lYOU ARE THE IMPOSTER"),
                        Component.text("\u00A77Eliminate the crew before they finish the task."),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
                ));
                if (numImposters > 1) {
                    String fellowImposters = players.stream()
                            .filter(other -> !other.equals(id) && roles.get(other) == Role.IMPOSTER)
                            .map(this::nameOf)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("none");
                    p.sendMessage("\u00A7cYour fellow Imposter(s): \u00A7f" + fellowImposters);
                }
            } else {
                p.showTitle(Title.title(
                        Component.text("\u00A7a\u00A7lYOU ARE A CREWMATE"),
                        Component.text("\u00A77Equip a villager in the End: Netherite-trim Diamond Leggings, Swift Sneak III"),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
                ));
            }
        }

        broadcastSystemMessage("\u00A76A game of Imposter has started with " + players.size() + " players. Good luck.");
    }

    public void forceStop(String reason) {
        state = GameState.ENDED;
        for (UUID id : lobby) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage("\u00A7cGame stopped: " + reason);
        }
        resetToWaiting();
    }

    private void resetToWaiting() {
        state = GameState.WAITING;
        roles.clear();
        eliminated.clear();
        bellUsed.clear();
        taskCompleted = false;
        activeMeeting = null;
    }

    // ---------- Gameplay events ----------

    public void handleTaskCompleted(Player completer) {
        if (state != GameState.RUNNING || taskCompleted) return;
        taskCompleted = true;
        broadcastSystemMessage("\u00A7b" + completer.getName() + " has completed the crew task! Find and vote out the Imposter to win.");
        checkWinConditions();
    }

    public void handleKill(Player victim, Player killer) {
        if (state != GameState.RUNNING) return;
        if (!isAlive(victim.getUniqueId())) return;
        Role killerRole = roles.get(killer.getUniqueId());
        if (killerRole != Role.IMPOSTER) return; // only imposter's kills count

        eliminated.add(victim.getUniqueId());
        victim.setGameMode(GameMode.SPECTATOR);
        broadcastSystemMessage("\u00A74A body has been found... " + victim.getName() + " was eliminated.");
        checkWinConditions();
    }

    public boolean useBell(Player player) {
        if (state != GameState.RUNNING) return false;
        if (activeMeeting != null) return false;
        if (!isAlive(player.getUniqueId())) return false;
        if (bellUsed.contains(player.getUniqueId())) return false;

        bellUsed.add(player.getUniqueId());
        callMeeting(player);
        return true;
    }

    private void callMeeting(Player caller) {
        state = GameState.MEETING;
        activeMeeting = new Meeting(plugin, this, caller.getUniqueId());
        broadcastSystemMessage("\u00A7c\u00A7l" + caller.getName() + " called an emergency meeting!");
        activeMeeting.begin();
    }

    public void onMeetingConcluded(Meeting meeting, UUID ejected) {
        if (ejected != null) {
            eliminated.add(ejected);
            Role role = roles.get(ejected);
            Player ejectedPlayer = Bukkit.getPlayer(ejected);
            if (ejectedPlayer != null) ejectedPlayer.setGameMode(GameMode.SPECTATOR);

            if (role == Role.IMPOSTER) {
                broadcastSystemMessage("\u00A7c" + nameOf(ejected) + " was an Imposter! They have been ejected.");
            } else {
                broadcastSystemMessage("\u00A7e" + nameOf(ejected) + " was NOT an Imposter. They have been ejected.");
            }
        } else {
            broadcastSystemMessage("\u00A77The vote was skipped or resulted in a tie. No one was ejected.");
        }

        activeMeeting = null;
        state = GameState.RUNNING;
        checkWinConditions();
    }

    private void checkWinConditions() {
        if (state == GameState.ENDED) return;

        long crewAlive = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.CREWMATE)
                .filter(e -> isAlive(e.getKey()))
                .count();

        long impostersAlive = roles.entrySet().stream()
                .filter(e -> e.getValue() == Role.IMPOSTER)
                .filter(e -> isAlive(e.getKey()))
                .count();

        if (crewAlive == 0) {
            endGame("\u00A74\u00A7lIMPOSTER WINS! \u00A7cAll crewmates were eliminated.");
            return;
        }

        if (impostersAlive == 0 && taskCompleted) {
            endGame("\u00A7a\u00A7lCREWMATES WIN! \u00A7aThe task was completed and every Imposter was ejected.");
            return;
        }

        // If every Imposter is already ejected but the task isn't done yet,
        // the kill threat is gone - crew just needs to finish the task, and
        // handleTaskCompleted() will trigger this same check again then.
    }

    private void endGame(String message) {
        state = GameState.ENDED;
        broadcastSystemMessage(message);

        for (UUID id : lobby) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Role role = roles.getOrDefault(id, Role.SPECTATOR);
            p.showTitle(Title.title(
                    Component.text(message.contains("IMPOSTER WINS") ? "\u00A74GAME OVER" : "\u00A7aGAME OVER"),
                    Component.text(role == Role.IMPOSTER ? "\u00A77You were the Imposter" : "\u00A77You were a Crewmate"),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            ));
            p.setGameMode(GameMode.SURVIVAL);
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::resetToWaiting, 200L);
    }

    // ---------- Helpers ----------

    public boolean isAlive(UUID id) {
        return roles.containsKey(id) && !eliminated.contains(id);
    }

    public List<UUID> getAlivePlayers() {
        List<UUID> alive = new ArrayList<>();
        for (UUID id : roles.keySet()) {
            if (isAlive(id)) alive.add(id);
        }
        return alive;
    }

    public GameState getState() {
        return state;
    }

    public Role getRole(UUID id) {
        return roles.getOrDefault(id, Role.SPECTATOR);
    }

    public boolean isInGame(UUID id) {
        return roles.containsKey(id);
    }

    private String nameOf(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) return p.getName();
        var op = Bukkit.getOfflinePlayer(id);
        return op.getName() != null ? op.getName() : id.toString();
    }

    /** Sends a plugin/system broadcast. This is NOT affected by the chat-disable rule,
     *  which only blocks player-typed chat messages. */
    public void broadcastSystemMessage(String legacyText) {
        for (UUID id : lobby) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(legacyText);
        }
        plugin.getLogger().info(org.bukkit.ChatColor.stripColor(legacyText));
    }

    public Location getGameSpawnLocation() {
        FileConfiguration cfg = plugin.getConfig();
        return new Location(
                Bukkit.getWorld(cfg.getString("arena-world", "world")),
                cfg.getDouble("game-spawn.x"),
                cfg.getDouble("game-spawn.y"),
                cfg.getDouble("game-spawn.z"),
                (float) cfg.getDouble("game-spawn.yaw"),
                0f
        );
    }

    public Location getVotingRoomLocation() {
        FileConfiguration cfg = plugin.getConfig();
        return new Location(
                Bukkit.getWorld(cfg.getString("arena-world", "world")),
                cfg.getDouble("voting-room.x"),
                cfg.getDouble("voting-room.y"),
                cfg.getDouble("voting-room.z"),
                (float) cfg.getDouble("voting-room.yaw"),
                0f
        );
    }

    public Meeting getActiveMeeting() {
        return activeMeeting;
    }
}
