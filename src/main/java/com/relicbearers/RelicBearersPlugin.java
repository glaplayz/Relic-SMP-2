package com.relicbearers;

import com.relicbearers.commands.RelicCommand;
import com.relicbearers.listeners.*;
import com.relicbearers.relic.RelicManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RelicBearersPlugin extends JavaPlugin {

    private RelicManager relicManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.relicManager = new RelicManager(this);

        getServer().getPluginManager().registerEvents(new JoinLeaveListener(this, relicManager), this);
        getServer().getPluginManager().registerEvents(new EnergyListener(relicManager), this);
        getServer().getPluginManager().registerEvents(new AbilityListener(relicManager), this);
        getServer().getPluginManager().registerEvents(new PvpListener(this, relicManager), this);
        getServer().getPluginManager().registerEvents(new RelicProtectionListener(this, relicManager), this);

        RerollListener rerollListener = new RerollListener(this, relicManager);
        getServer().getPluginManager().registerEvents(rerollListener, this);

        RelicCommand relicCommand = new RelicCommand(this, relicManager);
        getCommand("relic").setExecutor(relicCommand);
        getCommand("relic").setTabCompleter(relicCommand);

        // Passive regen + passive ability effects, every 4 seconds (matches config default)
        int interval = getConfig().getInt("charge.passive-regen-interval-seconds", 4) * 20;
        getServer().getScheduler().runTaskTimer(this, () -> {
            int regenAmount = getConfig().getInt("charge.passive-regen-amount", 1);
            for (Player player : getServer().getOnlinePlayers()) {
                relicManager.getProfile(player).getOwnedRelics().forEach(type ->
                        relicManager.addCharge(player, type, regenAmount));
                relicManager.applyPassives(player);
            }
        }, interval, interval);

        // Periodic autosave every 5 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this, relicManager::saveAll, 20L * 60 * 5, 20L * 60 * 5);

        getLogger().info("RelicBearers enabled. global-pvp-enabled=" + getConfig().getBoolean("global-pvp-enabled", true));
    }

    @Override
    public void onDisable() {
        if (relicManager != null) {
            relicManager.saveAll();
        }
    }

    public RelicManager getRelicManager() {
        return relicManager;
    }
}
