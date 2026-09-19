package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

import java.util.Map;

public class HopperLimiter implements Listener {
    private final PerformancePlus plugin;
    public HopperLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("hoppers")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.hoppers")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "hoppers", 8);
        if (limit <= 0) return;
        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.hoppers() >= limit) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "limites.hoppers",
                    Map.of("{limite}", String.valueOf(limit)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMinecartCreate(VehicleCreateEvent event) {
        if (event.getVehicle().getType() != EntityType.HOPPER_MINECART) return;
        Chunk chunk = event.getVehicle().getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("hoppers")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "hoppers", 8);
        if (limit <= 0) return;
        if (plugin.getMetricsManager().get(chunk).hoppers() >= limit) event.setCancelled(true);
    }
}
