package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AurumAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        double radius = 6 + tier * 2;
        Location loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 50, radius / 2, 1, radius / 2, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f, 1f);

        // Loot magnet: pull nearby dropped items toward the player
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Item item) {
                item.teleport(player.getLocation());
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 60, Math.min(2, tier - 1)));
    }

    @Override
    public void applyPassive(Player player, int tier) {
        // Bonus XP-from-kills/mining is applied in the energy listener directly,
        // since it needs to modify the XP orb amount at the source event.
    }
}
