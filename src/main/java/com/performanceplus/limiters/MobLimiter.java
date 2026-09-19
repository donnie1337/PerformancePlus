package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class MobLimiter implements Listener {
    private final PerformancePlus plugin;

    public MobLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("mobs")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "mobs", 60);
        if (limit <= 0) return;

        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.mobs() >= limit) event.setCancelled(true);
    }
}
