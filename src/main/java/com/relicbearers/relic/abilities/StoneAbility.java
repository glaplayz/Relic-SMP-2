package com.relicbearers.relic.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StoneAbility implements RelicAbility {

    @Override
    public void activate(Player player, int tier) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 60, 0.6, 1, 0.6, 0,
                org.bukkit.Material.STONE.createBlockData());
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_PLACE, 1.5f, 0.8f);

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * (4 + tier * 2), Math.min(1, tier - 1)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * (4 + tier * 2), 0));
    }

    // Passive mining fatigue immunity & flat damage reduction handled in listeners.
}
