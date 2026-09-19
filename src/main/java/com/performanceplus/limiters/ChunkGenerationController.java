package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkGenerationController implements Listener {
    private final PerformancePlus plugin;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public ChunkGenerationController(PerformancePlus plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::cleanup, 1200L, 1200L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk() || plugin.getConfigManager().isWorldIgnored(event.getWorld())) return;

        // Respeita tanto o limite global quanto uma configuração específica do mundo.
        // Isso permite que mundos criados/carregados pelo WorldPlus usem suas próprias regras.
        if (!plugin.getConfigManager().isLimitEnabled(event.getWorld(), "chunks-gerados-por-segundo")) return;

        int limit = plugin.getConfigManager().getLimit(event.getWorld(), "chunks-gerados-por-segundo", 20);
        if (limit <= 0) return;

        String key = event.getWorld().getUID().toString();
        long now = System.currentTimeMillis();
        Window w = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (w) {
            if (now - w.start >= 1000L) {
                w.start = now;
                w.count = 0;
            }
            w.count++;
            if (w.count == limit + 1) {
                plugin.getLogger().warning("Geração de chunks acima do limite configurado em "
                        + event.getWorld().getName() + ": " + limit + "/s.");
                if (plugin.getConfigManager().getBoolean("protecao.notificar-staff", true)) notifyStaff();
            }
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start > 3000L);
    }

    private void notifyStaff() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("performanceplus.notify")) {
                player.sendMessage(com.performanceplus.config.ConfigManager.color(
                        plugin.getConfigManager().getPrefix()
                                + "&eGeração de chunks acima do limite configurado."));
            }
        }
    }

    private static final class Window {
        private long start;
        private int count;
        private Window(long start) { this.start = start; }
    }
}
