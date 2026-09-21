package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobLimiter implements Listener {
    private final PerformancePlus plugin;

    public MobLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        Location location = event.getLocation();
        World world = location.getWorld();
        if (world == null
                || plugin.getConfigManager().isWorldIgnored(world)
                || !plugin.getConfigManager().isLimitEnabled(world, "mobs")) return;

        EntityType type = event.getEntityType();
        String mobKey = type.getKey().getKey();
        int limit = plugin.getConfigManager().getMobLimit(world, mobKey, 8);
        if (limit <= 0) return;

        double radius = plugin.getConfigManager().getMobRadius(world, mobKey, 16);
        double radiusSquared = radius * radius;
        int nearbySameType = 0;

        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity.equals(event.getEntity()) || entity.getType() != type) continue;
            if (entity.getLocation().distanceSquared(location) <= radiusSquared) {
                nearbySameType++;
            }
        }

        if (nearbySameType >= limit) {
            event.setCancelled(true);
        }
    }
}
