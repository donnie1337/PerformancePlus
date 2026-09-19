package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneLimiter implements Listener {
    private final PerformancePlus plugin;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();
    private long tick;

    public RedstoneLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            counts.clear();
            tick++;
        }, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onRedstone(BlockRedstoneEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("redstone")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "redstone", 25);
        if (limit <= 0) return;

        String key = ChunkUtils.key(chunk);
        int count = counts.merge(key, 1, Integer::sum);
        if (count <= limit) return;

        String action = plugin.getConfigManager().raw().getString(
                "limites.redstone.acao-quando-exceder", "bloquear-atualizacao");

        if ("bloquear-atualizacao".equalsIgnoreCase(action)) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }
}
