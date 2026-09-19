package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import org.bukkit.Chunk;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class XPLimiter implements Listener {
    private final PerformancePlus plugin;
    public XPLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() != EntityType.EXPERIENCE_ORB) return;
        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("xp-orbes")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "xp-orbes", 30);
        if (limit <= 0) return;
        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.xpOrbs() >= limit) event.setCancelled(true);
    }
}
