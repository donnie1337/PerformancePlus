package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class SpawnerLimiter implements Listener {
    private final PerformancePlus plugin;
    public SpawnerLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("spawners")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.spawners")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "spawners", 4);
        if (limit <= 0) return;

        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.spawners() >= limit) {
            event.setCancelled(true);
            MessageUtil.send(player, plugin.getConfigManager().getPrefix(),
                    "&cLimite de &f" + limit + " &cspawner(s) por chunk atingido.");
        }
    }
}
