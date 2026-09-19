package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Não escuta eventos diretamente — recebe contagens já calculadas pelo
 * ChunkScanTask (varredura periódica) e usa uma pontuação heurística
 * simples para sinalizar chunks que provavelmente são farms automáticas.
 *
 * A pontuação pesa mais hoppers/spawners/pistões/observers (indicadores
 * fortes de automação) do que mobs/itens sozinhos (que também aparecem em
 * áreas comuns do mapa, sem serem farms).
 */
public class FarmController {

    private final PerformancePlus plugin;
    private final Set<String> flaggedChunks = ConcurrentHashMap.newKeySet();

    public FarmController(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public void updateScore(Chunk chunk, int mobs, int items, int hoppers, int spawners) {
        if (!plugin.getConfigManager().getGlobalBoolean("farm-control.enabled", true)) {
            return;
        }
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int pistons = plugin.getPistonCount(chunk);
        int observers = plugin.getObserverCount(chunk);

        int score = (hoppers * 4) + (spawners * 6) + (pistons * 3) + (observers * 3) + mobs + (items / 2);

        int threshold = plugin.getConfigManager().getGlobalInt("farm-control.activity-threshold", 50);
        String key = ChunkUtils.key(chunk);

        if (score >= threshold) {
            boolean wasFlagged = flaggedChunks.contains(key);
            flaggedChunks.add(key);
            if (!wasFlagged) {
                notifyStaff(chunk, score);
            }
        } else {
            flaggedChunks.remove(key);
        }
    }

    private void notifyStaff(Chunk chunk, int score) {
        if (!plugin.getConfigManager().getGlobalBoolean("farm-control.notify-staff", true)) {
            return;
        }

        String msg = plugin.getConfigManager().getPrefix()
                + "&eProvável farm detectada em &f" + chunk.getWorld().getName()
                + " (" + chunk.getX() + ", " + chunk.getZ() + ") &e- pontuação: &f" + score;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("performanceplus.notify.farms")) {
                player.sendMessage(MessageUtil.color(msg));
            }
        }
        plugin.getLogger().info("Farm detectada em " + chunk.getWorld().getName()
                + " (" + chunk.getX() + ", " + chunk.getZ() + ") - pontuação: " + score);
    }

    public Set<String> getFlaggedChunks() {
        return flaggedChunks;
    }
}
