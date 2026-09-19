package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.CooldownTracker;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * BlockRedstoneEvent NÃO é cancelável — para "bloquear" uma atualização,
 * a técnica padrão é forçar newCurrent = oldCurrent, o que faz o Bukkit
 * tratar como se nada tivesse mudado.
 *
 * Este evento dispara com MUITA frequência (a cada mudança de nível de
 * corrente em cada bloco de redstone), então a contagem por chunk usa
 * um CooldownTracker leve (um Map + aritmética simples) para não pesar
 * no tick do servidor.
 */
public class RedstoneLimiter implements Listener {

    private final PerformancePlus plugin;
    private final CooldownTracker tracker = new CooldownTracker(1000L);

    public RedstoneLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }

        int perTickLimit = plugin.getConfigManager().getLimit(
                chunk.getWorld(), "redstone-updates-per-chunk-per-tick", 25);
        if (perTickLimit <= 0) {
            return;
        }

        // Convertido para uma janela de 1s (20 ticks) para reduzir o overhead
        // de reiniciar o contador a cada tick individual.
        int windowLimit = perTickLimit * 20;

        int count = tracker.registerAndCount(ChunkUtils.key(chunk));
        if (count > windowLimit) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }
}
