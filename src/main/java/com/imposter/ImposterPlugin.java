package com.imposter;

import com.imposter.commands.ImposterCommand;
import com.imposter.game.GameManager;
import com.imposter.listeners.ImposterWeaponListener;
import com.imposter.listeners.MeetingListener;
import com.imposter.listeners.PlayerListener;
import com.imposter.listeners.TaskListener;
import com.imposter.listeners.VotingGUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ImposterPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gameManager = new GameManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new MeetingListener(this), this);
        getServer().getPluginManager().registerEvents(new TaskListener(this), this);
        getServer().getPluginManager().registerEvents(new VotingGUIListener(), this);
        getServer().getPluginManager().registerEvents(new ImposterWeaponListener(this), this);

        ImposterCommand commandHandler = new ImposterCommand(this);
        getCommand("imposter").setExecutor(commandHandler);
        getCommand("startimposter").setExecutor(commandHandler);

        getLogger().info("ImposterPlugin enabled - chat disabled, death messages suppressed while active.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.forceStop("server shutting down / plugin disabled");
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
