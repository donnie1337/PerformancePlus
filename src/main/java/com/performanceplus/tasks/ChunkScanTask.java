package com.performanceplus.tasks;

import com.performanceplus.PerformancePlus;
import com.performanceplus.limiters.FarmController;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Roda periodicamente (intervalo configurável) sobre todos os chunks
 * carregados para:
 *  1) opcionalmente limpar itens dropados e orbes de XP excedentes
 *     (nunca mobs, para não arriscar matar animais/pets por engano);
 *  2) alimentar o FarmController com as contagens usadas na detecção
 *     heurística de farms automáticas.
 */
public class ChunkScanTask extends BukkitRunnable {

    private final PerformancePlus plugin;
    private final FarmController farmController;

    public ChunkScanTask(PerformancePlus plugin, FarmController farmController) {
        this.plugin = plugin;
        this.farmController = farmController;
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            if (plugin.getConfigManager().isWorldIgnored(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                scanChunk(chunk);
            }
        }
    }

    private void scanChunk(Chunk chunk) {
        int mobs = 0;
        int hoppers = 0;
        int spawners = 0;

        List<Entity> excessItems = new ArrayList<>();
        List<Entity> excessOrbs = new ArrayList<>();

        for (Entity e : chunk.getEntities()) {
            if (e.getType() == EntityType.DROPPED_ITEM) {
                excessItems.add(e);
            } else if (e.getType() == EntityType.EXPERIENCE_ORB) {
                excessOrbs.add(e);
            } else if (e instanceof LivingEntity && !(e instanceof Player)) {
                mobs++;
            }
        }

        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() == Material.HOPPER) {
                hoppers++;
            } else if (state.getType() == Material.SPAWNER) {
                spawners++;
            }
        }

        if (plugin.getConfigManager().getGlobalBoolean("settings.auto-cleanup.enabled", false)) {
            cleanupExcess(chunk, excessItems, "items-per-chunk", 40);
            cleanupExcess(chunk, excessOrbs, "xp-orbs-per-chunk", 30);
        }

        farmController.updateScore(chunk, mobs, excessItems.size(), hoppers, spawners);
    }

    private void cleanupExcess(Chunk chunk, List<Entity> entities, String key, int def) {
        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), key, def);
        if (limit <= 0 || entities.size() <= limit) {
            return;
        }

        int toRemove = entities.size() - limit;
        for (int i = 0; i < toRemove; i++) {
            entities.get(i).remove();
        }
    }
}
