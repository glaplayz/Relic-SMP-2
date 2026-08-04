package com.relicbearers.relic.abilities;

import org.bukkit.entity.Player;

/**
 * One active ability, triggered when a player right-clicks air/block while
 * holding their relic item in their offhand. `tier` (1-3) is used to scale
 * radius/duration/amplifier.
 */
public interface RelicAbility {

    void activate(Player player, int tier);

    /** Applies any passive, always-on effect. Called periodically (e.g. every few seconds). */
    default void applyPassive(Player player, int tier) {
        // no-op by default
    }
}
