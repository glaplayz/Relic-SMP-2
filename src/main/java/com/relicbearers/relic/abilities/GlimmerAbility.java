package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GlimmerAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        double radius = 5 + tier;
        Location loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 100, radius / 2, 1, radius / 2, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.5f, 2f);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * (3 + tier), 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * (10 + tier * 5), 0));
            }
        }
    }

    @Override
    public void applyPassive(Player player, int tier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 15, 0, true, false));
    }
}
