package com.imposter.commands;

import com.imposter.ImposterPlugin;
import com.imposter.game.GameState;
import com.imposter.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ImposterCommand implements CommandExecutor {

    private final ImposterPlugin plugin;

    public ImposterCommand(ImposterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /startimposter (and its aliases) is a shortcut straight to starting a game,
        // with an optional imposter-count argument: /startimposter [numberOfImposters]
        if (label.equalsIgnoreCase("startimposter")
                || label.equalsIgnoreCase("startliar")
                || label.equalsIgnoreCase("startgame")) {
            return handleStart(sender, args, 0);
        }

        if (args.length == 0) {
            sender.sendMessage("\u00A7cUsage: /imposter <start [imposters]|stop|join|leave|status|give-task-item|give-weapon>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                return handleStart(sender, args, 1);
            }
            case "stop" -> {
                if (!sender.hasPermission("imposter.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                plugin.getGameManager().forceStop("stopped by " + sender.getName());
            }
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("\u00A7cOnly players can join.");
                    return true;
                }
                if (plugin.getGameManager().join(player)) {
                    sender.sendMessage("\u00A7aYou joined the lobby (" + plugin.getGameManager().getLobby().size() + " players).");
                } else {
                    sender.sendMessage("\u00A7cCouldn't join - a game may already be running.");
                }
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("\u00A7cOnly players can leave.");
                    return true;
                }
                if (plugin.getGameManager().leave(player)) {
                    sender.sendMessage("\u00A7aYou left the lobby.");
                } else {
                    sender.sendMessage("\u00A7cYou weren't in the lobby.");
                }
            }
            case "status" -> {
                GameState state = plugin.getGameManager().getState();
                sender.sendMessage("\u00A76Game state: \u00A7f" + state
                        + " \u00A76| Lobby size: \u00A7f" + plugin.getGameManager().getLobby().size());
            }
            case "give-task-item" -> {
                if (!sender.hasPermission("imposter.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("\u00A7cOnly players can receive items.");
                    return true;
                }
                player.getInventory().addItem(ItemUtil.buildReferenceTaskItem());
                sender.sendMessage("\u00A7aGiven a reference task item (Netherite-trim Diamond Leggings, Swift Sneak III).");
            }
            case "give-weapon" -> {
                if (!sender.hasPermission("imposter.admin")) {
                    sender.sendMessage("\u00A7cYou don't have permission to do that.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("\u00A7cOnly players can receive items.");
                    return true;
                }
                int cooldown = plugin.getConfig().getInt("weapon-cooldown-seconds", 60);
                player.getInventory().addItem(ItemUtil.buildImposterWeapon(plugin, cooldown));
                sender.sendMessage("\u00A7aGiven a Silent Blade for testing (" + cooldown + "s cooldown).");
            }
            default -> sender.sendMessage("\u00A7cUsage: /imposter <start [imposters]|stop|join|leave|status|give-task-item|give-weapon>");
        }
        return true;
    }

    /**
     * Starts the game. If an argument is present at {@code countArgIndex}, it's parsed
     * as the number of secret Imposters for this round; otherwise the configured
     * default-imposters value is used.
     */
    private boolean handleStart(CommandSender sender, String[] args, int countArgIndex) {
        if (!sender.hasPermission("imposter.admin")) {
            sender.sendMessage("\u00A7cYou don't have permission to do that.");
            return true;
        }

        if (args.length > countArgIndex) {
            String raw = args[countArgIndex];
            try {
                int count = Integer.parseInt(raw);
                plugin.getGameManager().start(sender, count);
            } catch (NumberFormatException e) {
                sender.sendMessage("\u00A7c'" + raw + "' isn't a valid number of Imposters.");
            }
        } else {
            plugin.getGameManager().start(sender);
        }
        return true;
    }
}
