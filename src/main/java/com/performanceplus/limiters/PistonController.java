package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
    public void onPlace(BlockPlaceEvent event) {
        if (!isPiston(event.getBlock().getType())) return;
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                || !plugin.getConfigManager().isLimitEnabled("pistoes")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.pistoes")) return;

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "pistoes", 12);
        if (limit <= 0) return;
        Metrics m = plugin.getMetricsManager().get(chunk);
        if (m.pistons() >= limit) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "limites.pistoes",
                    Map.of("{limite}", String.valueOf(limit)));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
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

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "pistoes-ativacoes-por-segundo", 8);
        if (limit <= 0) return;

        String key = com.performanceplus.util.ChunkUtils.key(chunk);
        long now = System.currentTimeMillis();
        Window w = activity.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.start >= 1000L) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            if (w.count > limit) event.setCancelled(true);
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        activity.entrySet().removeIf(e -> now - e.getValue().start > 3000L);
    }

    private boolean isPiston(Material material) {
        return material == Material.PISTON || material == Material.STICKY_PISTON;
    }

    public int getCount(Chunk chunk) {
        return plugin.getMetricsManager().get(chunk).pistons();
    }

    private static final class Window {
        private long start;
        private int count;
        private Window(long start) { this.start = start; }
    }
}
