package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

/**
 * Limita o número TOTAL de entidades (de qualquer tipo) por chunk.
 *
 * O Bukkit não tem um único evento que cubra toda entidade possível:
 * mobs disparam CreatureSpawnEvent, itens disparam ItemSpawnEvent,
 * veículos (barcos, vagonetes) disparam VehicleCreateEvent, quadros e
 * pinturas disparam HangingPlaceEvent, e o restante (como orbes de XP)
 * cai no EntitySpawnEvent genérico. Cada um desses eventos tem sua
 * própria lista de handlers internamente, então é preciso escutar todos
 * eles separadamente para o limite geral funcionar de verdade.
 */
public class EntityLimiter implements Listener {

    private final PerformancePlus plugin;

    public EntityLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        checkLimit(event.getLocation().getChunk(), event, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        checkLimit(event.getLocation().getChunk(), event, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGenericSpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            return;
        }
        checkLimit(event.getLocation().getChunk(), event, null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        checkLimit(event.getEntity().getLocation().getChunk(), event, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        checkLimit(event.getVehicle().getLocation().getChunk(), event, null);
    }

    private void checkLimit(Chunk chunk, Cancellable event, Player player) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "entities-per-chunk", 100);
        if (limit <= 0) {
            return;
        }

        if (chunk.getEntities().length >= limit) {
            event.setCancelled(true);
            if (player != null) {
                MessageUtil.send(player, plugin.getConfigManager().getPrefix(),
                        "&cLimite de entidades neste chunk atingido!");
            }
        }
    }
}
