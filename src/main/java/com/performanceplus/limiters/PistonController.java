package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PistonController implements Listener {
    private final PerformancePlus plugin;
    private final Map<String, Window> activity = new ConcurrentHashMap<>();

    public PistonController(PerformancePlus plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanup, 1200L, 1200L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        checkActivity(event.getBlock().getChunk(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        checkActivity(event.getBlock().getChunk(), event);
    }

    private void checkActivity(Chunk chunk, org.bukkit.event.Cancellable event) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("pistoes-ativacoes-por-segundo")) return;

        int limit = plugin.getConfigManager().getFixedLimit(chunk.getWorld(), "pistoes-ativacoes-por-segundo", 8);
        if (limit <= 0) return;

        String key = com.performanceplus.util.ChunkUtils.key(chunk);
        long now = System.currentTimeMillis();
        Window window = activity.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.start >= 1000L) {
                window.start = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > limit) event.setCancelled(true);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        activity.entrySet().removeIf(entry -> now - entry.getValue().start > 3000L);
    }

    public int getCount(Chunk chunk) {
        return plugin.getMetricsManager().get(chunk).pistons();
    }

    private static final class Window {
        private long start;
        private int count;

        private Window(long start) {
            this.start = start;
        }
    }
}
