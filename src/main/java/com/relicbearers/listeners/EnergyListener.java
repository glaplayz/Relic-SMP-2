package com.relicbearers.listeners;

import com.relicbearers.relic.RelicManager;
import com.relicbearers.relic.RelicType;
import org.bukkit.Material;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.EnumSet;
import java.util.Set;

public class EnergyListener implements Listener {

    private final RelicManager relicManager;

    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE
    );

    public EnergyListener(RelicManager relicManager) {
        this.relicManager = relicManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (ORE_BLOCKS.contains(event.getBlock().getType())) {
            RelicType active = relicManager.getActiveRelicType(player);
            if (active != null) {
                relicManager.addMasteryXp(player, active, 2); // mirrors mastery.xp-mining-ore default
                relicManager.addCharge(player, active, 8);
            }
            return;
        }

        if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable ageable
                && ageable.getAge() == ageable.getMaximumAge()) {
            RelicType active = relicManager.getActiveRelicType(player);
            if (active != null) {
                relicManager.addMasteryXp(player, active, 2); // mirrors mastery.xp-harvest-crop default
                relicManager.addCharge(player, active, 8);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        // only hostile-ish PvE kills count (player kills are handled separately by PvpListener)
        if (event.getEntity() instanceof Player) return;
        RelicType active = relicManager.getActiveRelicType(killer);
        if (active == null) return;
        relicManager.addMasteryXp(killer, active, 4);
        relicManager.addCharge(killer, active, 8);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        RelicType active = relicManager.getActiveRelicType(player);
        if (active == null) return;
        relicManager.addMasteryXp(player, active, 3);
        relicManager.addCharge(player, active, 8);
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) return;
        RelicType active = relicManager.getActiveRelicType(player);
        if (active == null) return;
        relicManager.addMasteryXp(player, active, 6);
        relicManager.addCharge(player, active, 8);
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Advancement advancement = event.getAdvancement();
        if (advancement.getKey().getKey().startsWith("recipes/")) return; // ignore recipe unlock spam
        Player player = event.getPlayer();
        RelicType active = relicManager.getActiveRelicType(player);
        if (active == null) return;
        relicManager.addMasteryXp(player, active, 15);
        relicManager.addCharge(player, active, 8);
    }
}
