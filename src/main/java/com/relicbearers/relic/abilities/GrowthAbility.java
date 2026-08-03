package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GrowthAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        int radius = 3 + tier;
        Location loc = player.getLocation();

        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 40, radius / 2.0, 1, radius / 2.0, 0);
        loc.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1.5f, 1f);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    Block block = loc.clone().add(x, y, z).getBlock();
                    if (block.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(ageable.getMaximumAge());
                        block.setBlockData(ageable);
                    }
                }
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * (3 + tier), 1));

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity target && e != player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 4, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 4, 0));
            }
        }
    }

    @Override
    public void applyPassive(Player player, int tier) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 5, 0, true, false));
    }
}
