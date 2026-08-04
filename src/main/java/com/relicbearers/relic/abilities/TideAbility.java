package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class TideAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        double radius = 4 + tier;
        Location loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.SPLASH, loc, 100, radius / 2, 0.5, radius / 2, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_DOLPHIN_SPLASH, 1.5f, 1f);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2 + tier * 0.3);
                push.setY(0.3);
                target.setVelocity(push);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * (3 + tier), 1));
            }
        }
    }

    @Override
    public void applyPassive(Player player, int tier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 15, 0, true, false));
        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 15, 0, true, false));
        }
    }
}
