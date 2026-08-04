package com.relicbearers.relic;

import com.relicbearers.relic.abilities.*;
import com.relicbearers.storage.PlayerDataStore;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class RelicManager {

    private final JavaPlugin plugin;
    private final PlayerDataStore store;
    private final Map<UUID, RelicProfile> profiles = new HashMap<>();
    private final Map<RelicType, RelicAbility> abilities = new EnumMap<>(RelicType.class);
    private final NamespacedKey relicKey;
    private final Random random = new Random();

    // config-driven values, loaded once in constructor for simplicity
    private final int chargeMax;
    private final int abilityCost;
    private final int tier2Threshold;
    private final int tier3Threshold;
    private final int cooldownBase;
    private final int cooldownReductionPerTier;

    public RelicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.store = new PlayerDataStore(plugin);
        this.relicKey = new NamespacedKey(plugin, "relic_type");

        abilities.put(RelicType.EMBER, new EmberAbility());
        abilities.put(RelicType.TIDE, new TideAbility());
        abilities.put(RelicType.GROWTH, new GrowthAbility());
        abilities.put(RelicType.GUST, new GustAbility());
        abilities.put(RelicType.STONE, new StoneAbility());
        abilities.put(RelicType.GLIMMER, new GlimmerAbility());
        abilities.put(RelicType.VOID, new VoidAbility());
        abilities.put(RelicType.AURUM, new AurumAbility());

        this.chargeMax = plugin.getConfig().getInt("charge.max", 100);
        this.abilityCost = plugin.getConfig().getInt("charge.ability-cost", 40);
        this.tier2Threshold = plugin.getConfig().getInt("mastery.tier-2-threshold", 300);
        this.tier3Threshold = plugin.getConfig().getInt("mastery.tier-3-threshold", 1000);
        this.cooldownBase = plugin.getConfig().getInt("cooldown.base-seconds", 20);
        this.cooldownReductionPerTier = plugin.getConfig().getInt("cooldown.reduction-per-tier-seconds", 4);
    }

    public NamespacedKey getRelicKey() {
        return relicKey;
    }

    public RelicProfile getProfile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), id -> store.load(id));
    }

    public void unloadProfile(Player player) {
        RelicProfile profile = profiles.remove(player.getUniqueId());
        if (profile != null) {
            store.save(profile);
        }
    }

    public void saveAll() {
        for (RelicProfile profile : profiles.values()) {
            store.save(profile);
        }
    }

    /** Assigns a random starting relic if the player doesn't have one yet. Returns true if newly assigned. */
    public boolean ensureStartingRelic(Player player) {
        RelicProfile profile = getProfile(player);
        if (profile.getPrimaryRelic() != null) {
            return false;
        }
        RelicType[] all = RelicType.values();
        RelicType chosen = all[random.nextInt(all.length)];
        profile.setPrimaryRelic(chosen);
        giveRelicItem(player, chosen);
        return true;
    }

    public ItemStack createRelicItem(RelicType type) {
        ItemStack item = new ItemStack(type.getItemMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getColor() + "Relic of " + type.getDisplayName());
        meta.setLore(List.of(
                ChatColor.GRAY + type.getDescription(),
                ChatColor.DARK_GRAY + "Hold in offhand, right-click to activate."
        ));
        meta.getPersistentDataContainer().set(relicKey, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public void giveRelicItem(Player player, RelicType type) {
        player.getInventory().setItemInOffHand(createRelicItem(type));
    }

    public Optional<RelicType> getRelicTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer().get(relicKey, PersistentDataType.STRING);
        if (value == null) return Optional.empty();
        try {
            return Optional.of(RelicType.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * The relic type that should receive PvE mastery XP / charge / ability activation:
     * whichever relic item is currently in the player's offhand, if it's one they own,
     * otherwise their primary relic. This means growing a non-primary relic requires
     * actually equipping it.
     */
    public RelicType getActiveRelicType(Player player) {
        RelicProfile profile = getProfile(player);
        Optional<RelicType> fromOffhand = getRelicTypeFromItem(player.getInventory().getItemInOffHand());
        if (fromOffhand.isPresent() && profile.owns(fromOffhand.get())) {
            return fromOffhand.get();
        }
        return profile.getPrimaryRelic();
    }

    // --- Mastery XP / Tier progression ---

    public void addMasteryXp(Player player, RelicType type, int amount) {
        RelicProfile profile = getProfile(player);
        if (!profile.owns(type)) return;
        profile.addMasteryXp(type, amount);
        checkTierUp(player, profile, type);
    }

    private void checkTierUp(Player player, RelicProfile profile, RelicType type) {
        int xp = profile.getMasteryXp(type);
        int currentTier = profile.getTier(type);
        int newTier = currentTier;
        if (xp >= tier3Threshold) newTier = 3;
        else if (xp >= tier2Threshold) newTier = 2;

        if (newTier > currentTier) {
            profile.setTier(type, newTier);
            player.sendMessage(type.getColor() + "Your " + type.getDisplayName() +
                    " relic has grown to Tier " + newTier + "!" + ChatColor.RESET);
        }
    }

    // --- Charge ---

    public boolean spendCharge(Player player, RelicType type, int amount) {
        RelicProfile profile = getProfile(player);
        int current = profile.getCharge(type);
        if (current < amount) return false;
        profile.setCharge(type, current - amount);
        return true;
    }

    public void addCharge(Player player, RelicType type, int amount) {
        RelicProfile profile = getProfile(player);
        if (!profile.owns(type)) return;
        profile.addCharge(type, amount, chargeMax);
    }

    public int getChargeMax() {
        return chargeMax;
    }

    // --- Abilities ---

    /** Attempts to activate the ability for `type`. Returns a result message to send to the player, or null if it fired. */
    public String tryActivate(Player player, RelicType type) {
        RelicProfile profile = getProfile(player);
        if (!profile.owns(type)) {
            return ChatColor.RED + "You don't own the " + type.getDisplayName() + " relic.";
        }
        if (!profile.isAbilityOffCooldown(type)) {
            return ChatColor.RED + "Ability on cooldown: " + profile.getAbilityCooldownRemainingSeconds(type) + "s remaining.";
        }
        if (profile.getCharge(type) < abilityCost) {
            return ChatColor.RED + "Not enough charge (" + profile.getCharge(type) + "/" + abilityCost + ").";
        }

        RelicAbility ability = abilities.get(type);
        if (ability == null) return ChatColor.RED + "This relic has no ability implemented yet.";

        int tier = profile.getTier(type);
        ability.activate(player, tier);
        spendCharge(player, type, abilityCost);

        int cooldown = Math.max(4, cooldownBase - (tier - 1) * cooldownReductionPerTier);
        profile.setAbilityCooldown(type, cooldown);

        return null; // success, no error message
    }

    public void applyPassives(Player player) {
        RelicProfile profile = getProfile(player);
        for (RelicType type : profile.getOwnedRelics()) {
            RelicAbility ability = abilities.get(type);
            if (ability != null) {
                ability.applyPassive(player, profile.getTier(type));
            }
        }
    }

    // --- Kill-based relic transfer (the "collect all relics" engine) ---

    /**
     * Called when `winner` kills `loser` in normal PvP. Grants the winner the
     * loser's relic at Tier 1, unless the winner already owns it or the
     * loser's relic wasn't leveled enough (kill.min-tier-for-relic-drop).
     */
    public void awardRelicFromKill(Player winner, Player loser) {
        RelicProfile winnerProfile = getProfile(winner);
        RelicProfile loserProfile = getProfile(loser);
        RelicType loserRelic = loserProfile.getPrimaryRelic();
        if (loserRelic == null) return;

        int minTier = plugin.getConfig().getInt("kill.min-tier-for-relic-drop", 2);
        if (loserProfile.getTier(loserRelic) < minTier) {
            winner.sendMessage(ChatColor.YELLOW + loser.getName() +
                    "'s relic wasn't strong enough (Tier " + minTier + "+ required) — nothing dropped.");
            return;
        }

        if (winnerProfile.owns(loserRelic)) {
            winner.sendMessage(ChatColor.YELLOW + "You already own the " + loserRelic.getDisplayName() + " relic.");
            return;
        }

        winnerProfile.addOwnedRelic(loserRelic);
        winnerProfile.setTier(loserRelic, 1);
        winner.sendMessage(loserRelic.getColor() + "You claimed the " + loserRelic.getDisplayName() +
                " relic from " + loser.getName() + "! (Tier 1)");

        if (winnerProfile.hasAllRelics()) {
            winner.getServer().broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD +
                    winner.getName() + " has claimed all 8 relics and become a true Relic Bearer!");
        }
    }

    // --- Reroll (crafting recipe) ---

    /**
     * Rerolls the given owned relic type into a new random type (never the same one).
     * Progress (tier/xp/charge) on the old relic is lost; the new relic starts at Tier 1.
     * Returns the newly assigned relic type, or null if the player didn't own oldType.
     */
    public RelicType rerollRelic(Player player, RelicType oldType) {
        RelicProfile profile = getProfile(player);
        if (!profile.owns(oldType)) return null;

        boolean wasPrimary = oldType.equals(profile.getPrimaryRelic());

        List<RelicType> candidates = new ArrayList<>(List.of(RelicType.values()));
        candidates.remove(oldType);
        candidates.removeIf(profile::owns); // don't reroll into a relic they already own
        RelicType newType = candidates.isEmpty()
                ? oldType // edge case: player already owns every other relic type, reroll is a no-op type-wise
                : candidates.get(random.nextInt(candidates.size()));

        profile.removeOwnedRelic(oldType);
        if (wasPrimary || profile.getPrimaryRelic() == null) {
            profile.setPrimaryRelic(newType);
        } else {
            profile.addOwnedRelic(newType);
        }
        return newType;
    }
}
