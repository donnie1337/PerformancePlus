package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ItemLimiter implements Listener {

    private final PerformancePlus plugin;

    public ItemLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "items-per-chunk", 40);
        if (limit <= 0) {
            return;
        }

        long current = 0;
        for (Entity e : chunk.getEntities()) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                current++;
            }
        }

        if (current >= limit) {
            event.setCancelled(true);
        }
    }
}
