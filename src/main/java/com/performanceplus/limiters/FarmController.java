package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FarmController implements Listener {
    private final PerformancePlus plugin;
    private final Set<String> flaggedChunks = ConcurrentHashMap.newKeySet();

    public FarmController(PerformancePlus plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) { evaluate(event.getChunk()); }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) { evaluate(event.getBlock().getChunk()); }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) { evaluate(event.getBlock().getChunk()); }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) { evaluate(event.getLocation().getChunk()); }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        if (event.getEntity().getLocation().getWorld() != null) {
            evaluate(event.getEntity().getLocation().getChunk());
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        flaggedChunks.remove(ChunkUtils.key(event.getChunk()));
    }

    public void evaluate(Chunk chunk) {
        if (!plugin.getConfigManager().getBoolean("farm.habilitado", true)
                || plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) return;

        Metrics m = plugin.getMetricsManager().get(chunk);
        int score = m.mobs()
                + (m.items() / 2)
                + (m.hoppers() * 4)
                + (m.spawners() * 6)
                + (m.pistons() * 3)
                + (m.observers() * 3);

        int threshold = plugin.getConfigManager().getLimit(chunk.getWorld(), "farm-pontuacao", 100);
        String key = ChunkUtils.key(chunk);

        if (threshold > 0 && score >= threshold) {
            if (flaggedChunks.add(key)) notifyStaff(chunk, score);
        } else {
            flaggedChunks.remove(key);
        }
    }

    private void notifyStaff(Chunk chunk, int score) {
        if (!plugin.getConfigManager().getBoolean("farm.notificar-staff", true)) return;
        String msg = plugin.getConfigManager().getPrefix()
                + "&eChunk com alta concentração de recursos: &f"
                + chunk.getWorld().getName() + " " + chunk.getX() + "," + chunk.getZ()
                + " &7(pontuação " + score + ").";
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("performanceplus.notify.farms")) {
                player.sendMessage(MessageUtil.color(msg));
            }
        }
        plugin.getLogger().warning("Chunk de alta concentração: " + ChunkUtils.key(chunk) + " score=" + score);
    }

    public Set<String> getFlaggedChunks() { return flaggedChunks; }
}
