package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.CooldownTracker;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * Limita atualizações de redstone por chunk usando uma janela curta,
 * aproximando um tick (50 ms). A configuração continua expressa em
 * atualizações por chunk por tick; não há mais a conversão incorreta para
 * "valor x 20 por segundo".
 */
public class RedstoneLimiter implements Listener {

    private final PerformancePlus plugin;
    private final CooldownTracker tracker = new CooldownTracker(50L);

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

        int count = tracker.registerAndCount(ChunkUtils.key(chunk));
        if (count > perTickLimit) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }
}
