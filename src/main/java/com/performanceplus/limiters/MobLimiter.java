package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Limita quantos mobs (LivingEntity, exceto jogadores) podem existir em um
 * mesmo chunk. Alguns motivos de spawn ficam sempre liberados por padrão,
 * para não atrapalhar reprodução de animais e spawns disparados por comando
 * ou por outros plugins.
 */
public class MobLimiter implements Listener {

    private final PerformancePlus plugin;

    private static final Set<CreatureSpawnEvent.SpawnReason> ALWAYS_ALLOW = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.CUSTOM,
            CreatureSpawnEvent.SpawnReason.COMMAND,
            CreatureSpawnEvent.SpawnReason.BREEDING
    );

    public MobLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (ALWAYS_ALLOW.contains(event.getSpawnReason())) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "mobs-per-chunk", 60);
        if (limit <= 0) {
            return;
        }

        long current = 0;
        for (Entity e : chunk.getEntities()) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                current++;
            }
        }

        if (current >= limit) {
            event.setCancelled(true);
        }
    }
}
