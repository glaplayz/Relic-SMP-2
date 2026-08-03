package com.relicbearers.storage;

import com.relicbearers.relic.RelicProfile;
import com.relicbearers.relic.RelicType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persists each player's RelicProfile to plugins/RelicBearers/playerdata/<uuid>.yml
 */
public class PlayerDataStore {

    private final JavaPlugin plugin;
    private final File dataFolder;

    public PlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File fileFor(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    public RelicProfile load(UUID uuid) {
        File file = fileFor(uuid);
        RelicProfile profile = new RelicProfile(uuid);
        if (!file.exists()) {
            return profile; // fresh, caller should assign a starting relic
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        String primary = yml.getString("primaryRelic");
        if (primary != null) {
            try {
                profile.setPrimaryRelic(RelicType.valueOf(primary));
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<String> owned = yml.getStringList("ownedRelics");
        for (String s : owned) {
            try {
                profile.addOwnedRelic(RelicType.valueOf(s));
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (RelicType type : RelicType.values()) {
            String base = "relics." + type.name() + ".";
            if (yml.contains(base + "tier")) {
                profile.setTier(type, yml.getInt(base + "tier", 1));
                profile.addMasteryXp(type, yml.getInt(base + "xp", 0));
                profile.setCharge(type, yml.getInt(base + "charge", 0));
            }
        }

        return profile;
    }

    public void save(RelicProfile profile) {
        YamlConfiguration yml = new YamlConfiguration();

        if (profile.getPrimaryRelic() != null) {
            yml.set("primaryRelic", profile.getPrimaryRelic().name());
        }

        List<String> owned = profile.getOwnedRelics().stream().map(Enum::name).toList();
        yml.set("ownedRelics", owned);

        for (RelicType type : profile.getOwnedRelics()) {
            String base = "relics." + type.name() + ".";
            yml.set(base + "tier", profile.getTier(type));
            yml.set(base + "xp", profile.getMasteryXp(type));
            yml.set(base + "charge", profile.getCharge(type));
        }

        try {
            yml.save(fileFor(profile.getPlayerId()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save relic profile for " + profile.getPlayerId(), e);
        }
    }
}
