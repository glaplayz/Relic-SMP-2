package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EmberAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        double radius = 3 + tier; // 4, 5, 6
        Location loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.FLAME, loc, 80, radius / 2, 1, radius / 2, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.8f);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                target.setFireTicks(60 + tier * 40);
                target.damage(2 + tier, player);
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 5, 0));
    }

    @Override
    public void applyPassive(Player player, int tier) {
        // Reduces fire damage taken; simplest reliable implementation is periodic
        // fire resistance pulses handled by the listener when fire damage occurs,
        // so this passive is mostly informational / cosmetic here.
    }
}
