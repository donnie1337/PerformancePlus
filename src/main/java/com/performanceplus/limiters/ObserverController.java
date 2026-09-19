package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;

public class ObserverController implements Listener {
    private final PerformancePlus plugin;

    public ObserverController(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.OBSERVER) return;
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("observers")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.observers")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "observers", 12);
        if (limit <= 0) return;
        Metrics m = plugin.getMetricsManager().get(chunk);
        if (m.observers() >= limit) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "limites.observers",
                    Map.of("{limite}", String.valueOf(limit)));
        }
    }

    public int getCount(Chunk chunk) {
        return plugin.getMetricsManager().get(chunk).observers();
    }
}
