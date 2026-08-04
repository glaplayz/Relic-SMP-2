package com.relicbearers.commands;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicProfile;
import com.relicbearers.relic.RelicType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RelicCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;

    public RelicCommand(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "info" -> handleInfo(sender);
            case "list" -> handleList(sender);
            case "switch" -> handleSwitch(sender, args);
            case "give" -> handleGive(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- RelicBearers ---");
        sender.sendMessage(ChatColor.YELLOW + "/relic info" + ChatColor.GRAY + " - view your relics, tiers, and charge");
        sender.sendMessage(ChatColor.YELLOW + "/relic list" + ChatColor.GRAY + " - list all 8 relic types");
        sender.sendMessage(ChatColor.YELLOW + "/relic switch <type>" + ChatColor.GRAY + " - equip a relic you own into your offhand");
        sender.sendMessage(ChatColor.GRAY + "Kill another player to claim their relic (Tier 1). Craft a relic item + " +
                "catalyst to reroll it into a new one.");
        if (sender.hasPermission("relicbearers.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/relic give <player> <type>" + ChatColor.GRAY + " - [admin] give a relic item");
        }
    }

    private void handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        RelicProfile profile = relicManager.getProfile(player);

        sender.sendMessage(ChatColor.GOLD + "--- Your Relics ---");
        for (RelicType type : RelicType.values()) {
            if (!profile.owns(type)) continue;
            String primaryTag = type == profile.getPrimaryRelic() ? ChatColor.AQUA + " (primary)" : "";
            sender.sendMessage(type.coloredName() + primaryTag + ChatColor.GRAY +
                    " - Tier " + profile.getTier(type) +
                    " | XP " + profile.getMasteryXp(type) +
                    " | Charge " + profile.getCharge(type) + "/" + relicManager.getChargeMax());
        }

        if (profile.hasAllRelics()) {
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have claimed every relic!");
        } else {
            sender.sendMessage(ChatColor.GRAY + "Relics owned: " + profile.getOwnedRelics().size() + "/" + RelicType.values().length);
        }
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- All Relics ---");
        for (RelicType type : RelicType.values()) {
            sender.sendMessage(type.coloredName() + ChatColor.GRAY + " - " + type.getDescription());
        }
    }

    private void handleSwitch(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /relic switch <type>");
            return;
        }
        RelicType type = parseType(args[1]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown relic type. See /relic list.");
            return;
        }
        RelicProfile profile = relicManager.getProfile(player);
        if (!profile.owns(type)) {
            sender.sendMessage(ChatColor.RED + "You don't own the " + type.getDisplayName() + " relic yet.");
            return;
        }
        relicManager.giveRelicItem(player, type);
        sender.sendMessage(type.getColor() + "Equipped the " + type.getDisplayName() + " relic in your offhand.");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("relicbearers.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /relic give <player> <type>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        RelicType type = parseType(args[2]);
        if (type == null) {
            sender.sendMessage(ChatColor.RED + "Unknown relic type. See /relic list.");
            return;
        }
        RelicProfile profile = relicManager.getProfile(target);
        profile.addOwnedRelic(type);
        relicManager.giveRelicItem(target, type);
        sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " the " + type.getDisplayName() + " relic.");
        target.sendMessage(type.getColor() + "You were granted the " + type.getDisplayName() + " relic!");
    }

    private RelicType parseType(String input) {
        try {
            return RelicType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("info", "list", "switch"));
            if (sender.hasPermission("relicbearers.admin")) {
                options.add("give");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            } else if (sub.equals("switch")) {
                return List.of(RelicType.values()).stream().map(Enum::name).collect(Collectors.toList());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return List.of(RelicType.values()).stream().map(Enum::name).collect(Collectors.toList());
        }
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
