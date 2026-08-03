package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class GustAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        Vector direction = player.getLocation().getDirection().normalize();
        double power = 1.4 + tier * 0.3;
        Vector boost = direction.multiply(power).setY(0.6 + tier * 0.1);
        player.setVelocity(boost);

        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, 60, 0.5, 0.5, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 1.3f);

        double radius = 3 + tier;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                Vector push = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5);
                push.setY(0.4);
                target.setVelocity(push);
            }
        }
    }

    // Fall damage reduction is handled in the damage listener, keyed off owning GUST.
}
