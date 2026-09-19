package com.performanceplus.monitor;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Calcula TPS e MSPT manualmente, sem depender de nenhuma API específica
 * do Paper (Bukkit.getTPS() só existe no Paper, não no Spigot puro).
 *
 * A técnica é simples: uma tarefa roda a CADA tick e mede quanto tempo se
 * passou desde o tick anterior (em nanosegundos); a cada 20 ticks (1s),
 * calculamos a média desse período e derivamos o TPS a partir dela.
 */
public class PerformanceMonitor {

    private final PerformancePlus plugin;

    private long lastTickTime = System.nanoTime();
    private double msptSum = 0;
    private int tickCount = 0;

    private volatile double currentTps = 20.0;
    private volatile double currentMspt = 50.0;

    private BukkitTask tickTask;
    private BukkitTask reportTask;

    public PerformanceMonitor(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        lastTickTime = System.nanoTime();

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.nanoTime();
            double deltaMillis = (now - lastTickTime) / 1_000_000.0;
            lastTickTime = now;

            msptSum += deltaMillis;
            tickCount++;
        }, 1L, 1L);

        reportTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tickCount > 0) {
                currentMspt = msptSum / tickCount;
                currentTps = Math.min(20.0, 1000.0 / currentMspt);
            }
            msptSum = 0;
            tickCount = 0;
            checkWarnings();
        }, 20L, 20L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (reportTask != null) {
            reportTask.cancel();
        }
    }

    public double getTps() {
        return currentTps;
    }

    public double getMspt() {
        return currentMspt;
    }

    private void checkWarnings() {
        if (!plugin.getConfigManager().getGlobalBoolean("monitoring.enabled", true)) {
            return;
        }

        double tpsThreshold = plugin.getConfigManager().getGlobalDouble("monitoring.tps-warning-threshold", 18.0);
        double msptThreshold = plugin.getConfigManager().getGlobalDouble("monitoring.mspt-warning-threshold", 50.0);

        if (currentTps < tpsThreshold || currentMspt > msptThreshold) {
            broadcastWarning();
        }
    }

    private void broadcastWarning() {
        String msg = plugin.getConfigManager().getPrefix()
                + "&eAlerta de performance! TPS: &f" + String.format("%.1f", currentTps)
                + " &e| MSPT: &f" + String.format("%.1f", currentMspt) + "ms";

        plugin.getLogger().warning("TPS baixo detectado: " + String.format("%.1f", currentTps)
                + " | MSPT: " + String.format("%.1f", currentMspt) + "ms");

        if (!plugin.getConfigManager().getGlobalBoolean("monitoring.broadcast-to-ops", true)) {
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("performanceplus.notify")) {
                player.sendMessage(MessageUtil.color(msg));
            }
        }
    }
}
