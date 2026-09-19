package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Map;

public class SpawnerLimiter implements Listener {
    private final PerformancePlus plugin;
    public SpawnerLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;
        var chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("spawners")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.spawners")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "spawners", 4);
        if (limit <= 0) return;

        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.spawners() >= limit) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "limites.spawners",
                    Map.of("{limite}", String.valueOf(limit)));
        }
    }
}
