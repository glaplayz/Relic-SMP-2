package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicType;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinLeaveListener implements Listener {

    private final JavaPlugin plugin;
    private final RelicManager relicManager;

    public JoinLeaveListener(JavaPlugin plugin, RelicManager relicManager) {
        this.plugin = plugin;
        this.relicManager = relicManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        boolean isNew = relicManager.ensureStartingRelic(event.getPlayer());
        if (isNew) {
            RelicType type = relicManager.getProfile(event.getPlayer()).getPrimaryRelic();
            event.getPlayer().sendMessage(ChatColor.GOLD + "You have been chosen as a bearer of the " +
                    type.coloredName() + ChatColor.GOLD + " relic!");
            event.getPlayer().sendMessage(ChatColor.GRAY + type.getDescription());
            event.getPlayer().sendMessage(ChatColor.GRAY + "Hold it in your offhand and right-click to use its ability. Type /relic info for details.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        relicManager.unloadProfile(event.getPlayer());
    }
}
