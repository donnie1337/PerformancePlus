package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

import java.util.Map;

public class EntityLimiter implements Listener {
    private final PerformancePlus plugin;
    public EntityLimiter(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        check(event.getLocation().getChunk(), event, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) return;
        check(event.getLocation().getChunk(), event, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingPlaceEvent event) {
        check(event.getEntity().getLocation().getChunk(), event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicle(VehicleCreateEvent event) {
        check(event.getVehicle().getLocation().getChunk(), event, null);
    }

    private void check(Chunk chunk, Cancellable event, Player player) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("entidades")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "entidades", 100);
        if (limit <= 0) return;

        Metrics metrics = plugin.getMetricsManager().get(chunk);
        if (metrics.entities() >= limit) {
            event.setCancelled(true);
            if (player != null) {
                plugin.getMessageManager().send(player, "limites.entidades", Map.of("{limite}", String.valueOf(limit)));
            }
        }
    }
}
