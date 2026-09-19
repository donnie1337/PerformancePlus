package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

public class ItemLimiter implements Listener {
    private final PerformancePlus plugin;
    public ItemLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Chunk chunk = event.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("itens")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "itens", 40);
        if (limit <= 0) return;
        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.items() >= limit) event.setCancelled(true);
    }
}
