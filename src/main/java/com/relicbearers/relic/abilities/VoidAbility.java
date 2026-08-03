package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;

public class VoidAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        double distance = 6 + tier * 2; // 8, 10, 12
        Location start = player.getEyeLocation();
        RayTraceResult trace = player.getWorld().rayTraceBlocks(start, start.getDirection(), distance);

        Location destination;
        if (trace != null && trace.getHitPosition() != null) {
            destination = trace.getHitPosition().toLocation(player.getWorld())
                    .subtract(start.getDirection().multiply(0.5));
            destination.setY(destination.getY());
            destination.setYaw(player.getLocation().getYaw());
            destination.setPitch(player.getLocation().getPitch());
        } else {
            destination = start.add(start.getDirection().multiply(distance));
        }

        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 60, 0.3, 0.5, 0.3, 0.3);
        player.teleport(destination);
        player.getWorld().spawnParticle(Particle.PORTAL, destination, 60, 0.3, 0.5, 0.3, 0.3);
        player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * (2 + tier), 0));
    }

    @Override
    public void applyPassive(Player player, int tier) {
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }
}
