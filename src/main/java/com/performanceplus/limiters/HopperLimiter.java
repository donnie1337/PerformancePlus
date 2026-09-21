package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

import java.util.Map;

public class HopperLimiter implements Listener {
    private final PerformancePlus plugin;

    public HopperLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;

        Chunk chunk = event.getBlock().getChunk();
        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.hoppers") || !isEnabled(chunk, "hoppers")) return;

        int limit = getLimit(chunk, "hoppers", 8);
        if (limit > 0 && countHopperBlocksBeforePlacement(chunk, event.getBlock()) >= limit) {
            event.setCancelled(true);
            sendLimit(player, limit, "Funis");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinecartInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getItem() == null
                || event.getItem().getType() != Material.HOPPER_MINECART) return;

        Player player = event.getPlayer();
        Chunk chunk = event.getClickedBlock().getChunk();
        if (player.hasPermission("performanceplus.bypass.hoppers") || !isEnabled(chunk, "carrinho-com-funil")) return;

        int limit = getLimit(chunk, "carrinho-com-funil", 8);
        if (limit > 0 && countHopperMinecarts(chunk) >= limit) {
            event.setCancelled(true);
            sendLimit(player, limit, "Carrinho com funil");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinecartCreate(VehicleCreateEvent event) {
        if (event.getVehicle().getType() != EntityType.HOPPER_MINECART) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        if (!isEnabled(chunk, "carrinho-com-funil")) return;

        int limit = getLimit(chunk, "carrinho-com-funil", 8);
        if (limit > 0 && countHopperMinecarts(chunk) >= limit) {
            event.setCancelled(true);
        }
    }

    private boolean isEnabled(Chunk chunk, String key) {
        return !plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                && plugin.getConfigManager().isLimitEnabled(chunk.getWorld(), key);
    }

    private int getLimit(Chunk chunk, String key, int fallback) {
        return plugin.getConfigManager().getFixedLimit(chunk.getWorld(), key, fallback);
    }

    private int countHopperBlocksBeforePlacement(Chunk chunk, org.bukkit.block.Block pendingBlock) {
        int total = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() != Material.HOPPER) continue;
            if (state.getX() == pendingBlock.getX()
                    && state.getY() == pendingBlock.getY()
                    && state.getZ() == pendingBlock.getZ()) continue;
            total++;
        }
        return total;
    }

    private int countHopperMinecarts(Chunk chunk) {
        int total = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.HOPPER_MINECART) total++;
        }
        return total;
    }

    private void sendLimit(Player player, int limit, String item) {
        plugin.getMessageManager().send(player, "limites.componente",
                Map.of("{limite}", String.valueOf(limit), "{item}", item));
    }
}
