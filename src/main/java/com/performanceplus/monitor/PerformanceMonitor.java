package com.performanceplus.monitor;

import com.performanceplus.PerformancePlus;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class PerformanceMonitor {
    private final PerformancePlus plugin;
    private long lastTickTime;
    private double msptSum;
    private int tickCount;
    private volatile double currentTps = 20.0;
    private volatile double currentMspt = 50.0;
    private volatile int protectionLevel = 0;
    private long lastWarningAt;

    private BukkitTask tickTask;
    private BukkitTask reportTask;

    public PerformanceMonitor(PerformancePlus plugin) { this.plugin = plugin; }

    public void start() {
        lastTickTime = System.nanoTime();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.nanoTime();
            double delta = (now - lastTickTime) / 1_000_000.0;
            lastTickTime = now;
            msptSum += delta;
            tickCount++;
        }, 1L, 1L);

        reportTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (tickCount > 0) {
                currentMspt = msptSum / tickCount;
                currentTps = Math.min(20.0, 1000.0 / Math.max(1.0, currentMspt));
            }
            msptSum = 0;
            tickCount = 0;
            updateProtectionLevel();
            checkWarning();
        }, 20L, 20L);
    }

    public void stop() {
        if (tickTask != null) tickTask.cancel();
        if (reportTask != null) reportTask.cancel();
    }

    public double getTps() { return currentTps; }
    public double getMspt() { return currentMspt; }
    public int getProtectionLevel() { return protectionLevel; }
    public boolean isProtectionActive() { return protectionLevel > 0; }

    private void updateProtectionLevel() {
        if (!plugin.getConfigManager().getBoolean("performance.protecao-adaptativa.habilitado", true)) {
            protectionLevel = 0;
            return;
        }

        double level3 = plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.critico.mspt", 100.0);
        double level2 = plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.alto.mspt", 75.0);
        double level1 = plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.atencao.mspt", 50.0);

        if (currentMspt >= level3) protectionLevel = 3;
        else if (currentMspt >= level2) protectionLevel = 2;
        else if (currentMspt >= level1) protectionLevel = 1;
        else protectionLevel = 0;
    }

    public double getLimitMultiplier() {
        return switch (protectionLevel) {
            case 3 -> plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.critico.multiplicador-limites", 0.50);
            case 2 -> plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.alto.multiplicador-limites", 0.75);
            case 1 -> plugin.getConfigManager().getDouble("performance.protecao-adaptativa.niveis.atencao.multiplicador-limites", 0.90);
            default -> 1.0;
        };
    }

    private void checkWarning() {
        if (!plugin.getConfigManager().getBoolean("monitoramento.habilitado", true)) return;
        double threshold = plugin.getConfigManager().getDouble("monitoramento.alerta-mspt", 50.0);
        if (currentMspt < threshold) return;

        long cooldown = plugin.getConfigManager().getInt("monitoramento.intervalo-alertas-segundos", 30) * 1000L;
        long now = System.currentTimeMillis();
        if (now - lastWarningAt < cooldown) return;
        lastWarningAt = now;

        Map<String, String> p = Map.of(
                "{nivel}", String.valueOf(protectionLevel),
                "{tps}", String.format("%.2f", currentTps),
                "{mspt}", String.format("%.2f", currentMspt));
        plugin.getLogger().info(plugin.getMessageManager().get("performance.console", p));

        if (!plugin.getConfigManager().getBoolean("monitoramento.notificar-staff", true)) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("performanceplus.notify")) {
                plugin.getMessageManager().send(player, "performance.staff", p);
            }
        }
    }
}
