package com.relicbearers.relic;

import java.util.*;

/**
 * All persistent state for one player:
 *  - which relic types they own (starts with exactly 1, random)
 *  - their tier and lifetime mastery XP per owned relic type
 *  - their current spendable charge per owned relic type
 *  - cooldown timestamps for ability use
 */
public class RelicProfile {

    private final UUID playerId;
    private RelicType primaryRelic;
    private final Set<RelicType> ownedRelics = new HashSet<>();

    private final Map<RelicType, Integer> tier = new HashMap<>();
    private final Map<RelicType, Integer> masteryXp = new HashMap<>();
    private final Map<RelicType, Integer> charge = new HashMap<>();

    // relicType -> epoch millis until ability usable again
    private final Map<RelicType, Long> abilityCooldownUntil = new HashMap<>();

    public RelicProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public RelicType getPrimaryRelic() {
        return primaryRelic;
    }

    public void setPrimaryRelic(RelicType type) {
        this.primaryRelic = type;
        addOwnedRelic(type);
    }

    public void addOwnedRelic(RelicType type) {
        ownedRelics.add(type);
        tier.putIfAbsent(type, 1);
        masteryXp.putIfAbsent(type, 0);
        charge.putIfAbsent(type, 0);
    }

    /** Fully removes a relic type from this profile (used when rerolling). */
    public void removeOwnedRelic(RelicType type) {
        ownedRelics.remove(type);
        tier.remove(type);
        masteryXp.remove(type);
        charge.remove(type);
        abilityCooldownUntil.remove(type);
        if (primaryRelic == type) {
            primaryRelic = null;
        }
    }

    public Set<RelicType> getOwnedRelics() {
        return ownedRelics;
    }

    public boolean owns(RelicType type) {
        return ownedRelics.contains(type);
    }

    public boolean hasAllRelics() {
        return ownedRelics.size() >= RelicType.values().length;
    }

    public int getTier(RelicType type) {
        return tier.getOrDefault(type, 1);
    }

    public void setTier(RelicType type, int value) {
        tier.put(type, value);
    }

    public int getMasteryXp(RelicType type) {
        return masteryXp.getOrDefault(type, 0);
    }

    public void addMasteryXp(RelicType type, int amount) {
        masteryXp.merge(type, amount, Integer::sum);
    }

    public int getCharge(RelicType type) {
        return charge.getOrDefault(type, 0);
    }

    public void setCharge(RelicType type, int value) {
        charge.put(type, value);
    }

    public void addCharge(RelicType type, int amount, int max) {
        int current = getCharge(type);
        charge.put(type, Math.min(max, current + amount));
    }

    public boolean isAbilityOffCooldown(RelicType type) {
        Long until = abilityCooldownUntil.get(type);
        return until == null || System.currentTimeMillis() >= until;
    }

    public long getAbilityCooldownRemainingSeconds(RelicType type) {
        Long until = abilityCooldownUntil.get(type);
        if (until == null) return 0;
        long remaining = until - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public void setAbilityCooldown(RelicType type, long seconds) {
        abilityCooldownUntil.put(type, System.currentTimeMillis() + seconds * 1000);
    }
}
