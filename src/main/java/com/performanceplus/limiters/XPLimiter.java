package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Orbes de experiência não têm um evento de spawn dedicado no Bukkit;
 * eles caem no EntitySpawnEvent genérico, então filtramos pelo tipo aqui.
 */
public class XPLimiter implements Listener {

    private final PerformancePlus plugin;

    public XPLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.EXPERIENCE_ORB) {
            return;
        }

        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "xp-orbs-per-chunk", 30);
        if (limit <= 0) {
            return;
        }

        long current = 0;
        for (Entity e : chunk.getEntities()) {
            if (e.getType() == EntityType.EXPERIENCE_ORB) {
                current++;
            }
        }

        if (current >= limit) {
            event.setCancelled(true);
        }
    }
}
