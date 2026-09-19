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
 * Varredura de farms/limpeza distribuída em pequenos lotes.
 *
 * A versão anterior varria TODOS os chunks carregados de uma vez a cada
 * intervalo, o que podia criar picos de MSPT em servidores grandes.
 * Agora os chunks são processados em lotes por tick. O intervalo continua
 * controlando a frequência de uma varredura completa.
 */
public class ChunkScanTask extends BukkitRunnable {

    private final PerformancePlus plugin;
    private final FarmController farmController;

    private final List<Chunk> queue = new ArrayList<>();
    private int queueIndex;
    private long nextScanAt;

    public ChunkScanTask(PerformancePlus plugin, FarmController farmController) {
        this.plugin = plugin;
        this.farmController = farmController;
        this.nextScanAt = System.currentTimeMillis();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        if (queueIndex >= queue.size()) {
            if (now < nextScanAt) {
                return;
            }
            rebuildQueue();
            if (queue.isEmpty()) {
                nextScanAt = now + getIntervalMillis();
                return;
            }
        }

        int budget = Math.max(1, plugin.getConfigManager()
                .getGlobalInt("settings.scan-chunks-per-tick", 25));

        int processed = 0;
        while (queueIndex < queue.size() && processed < budget) {
            Chunk chunk = queue.get(queueIndex++);
            if (chunk.isLoaded() && !plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
                scanChunk(chunk);
            }
            processed++;
        }

        if (queueIndex >= queue.size()) {
            queue.clear();
            queueIndex = 0;
            nextScanAt = System.currentTimeMillis() + getIntervalMillis();
        }
    }

    private void rebuildQueue() {
        queue.clear();
        queueIndex = 0;

        for (World world : plugin.getServer().getWorlds()) {
            if (plugin.getConfigManager().isWorldIgnored(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                queue.add(chunk);
            }
        }
    }

    private long getIntervalMillis() {
        int ticks = Math.max(20, plugin.getConfigManager()
                .getGlobalInt("settings.check-interval-ticks", 100));
        return ticks * 50L;
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
