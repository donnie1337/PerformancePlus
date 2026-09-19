package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

/**
 * Limita hoppers por chunk, contando tanto os hoppers normais (bloco)
 * quanto os vagonetes com funil (hopper minecart, que é uma entidade e
 * é criado via VehicleCreateEvent, não BlockPlaceEvent).
 */
public class HopperLimiter implements Listener {

    private final PerformancePlus plugin;

    public HopperLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.hoppers")) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "hoppers-per-chunk", 8);
        if (countHoppers(chunk) >= limit) {
            event.setCancelled(true);
            MessageUtil.send(player, plugin.getConfigManager().getPrefix(),
                    "&cLimite de &f" + limit + " &chopper(s) por chunk atingido!");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMinecartCreate(VehicleCreateEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (vehicle.getType() != EntityType.HOPPER_MINECART) {
            return;
        }

        Chunk chunk = vehicle.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "hoppers-per-chunk", 8);
        if (countHoppers(chunk) >= limit) {
            event.setCancelled(true);
        }
    }

    public static int countHoppers(Chunk chunk) {
        int count = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() == Material.HOPPER) {
                count++;
            }
        }
        for (Entity e : chunk.getEntities()) {
            if (e.getType() == EntityType.HOPPER_MINECART) {
                count++;
            }
        }
        return count;
    }
}
