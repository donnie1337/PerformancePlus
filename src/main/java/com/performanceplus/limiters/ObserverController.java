package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita observers por chunk usando o mesmo padrão de contador incremental
 * do PistonController. O controle de "pulsos por segundo" de um relógio de
 * observer fica coberto indiretamente pelo RedstoneLimiter, já que cada
 * pulso de um observer gera atualizações de redstone nos blocos vizinhos.
 */
public class ObserverController implements Listener {

    private final PerformancePlus plugin;
    private final ConcurrentHashMap<String, Integer> placedCount = new ConcurrentHashMap<>();

    public ObserverController(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.OBSERVER) {
            return;
        }

        Player player = event.getPlayer();
        Chunk chunk = block.getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }
        if (player.hasPermission("performanceplus.bypass.observers")) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "observers-per-chunk", 12);
        String key = ChunkUtils.key(chunk);
        int current = placedCount.getOrDefault(key, 0);

        if (current >= limit) {
            event.setCancelled(true);
            MessageUtil.send(player, plugin.getConfigManager().getPrefix(),
                    "&cLimite de &f" + limit + " &cobserver(s) por chunk atingido!");
            return;
        }
        placedCount.merge(key, 1, Integer::sum);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.OBSERVER) {
            return;
        }

        String key = ChunkUtils.key(block.getChunk());
        placedCount.computeIfPresent(key, (k, v) -> Math.max(0, v - 1));
    }

    public int getCount(Chunk chunk) {
        return placedCount.getOrDefault(ChunkUtils.key(chunk), 0);
    }

    /**
     * Refaz a contagem de observers varrendo bloco a bloco todos os chunks
     * carregados. Operação pesada — use apenas sob demanda (/pperf recount).
     */
    public void recountAll() {
        placedCount.clear();
        for (World world : plugin.getServer().getWorlds()) {
            if (plugin.getConfigManager().isWorldIgnored(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                int count = countObserversInChunk(chunk);
                if (count > 0) {
                    placedCount.put(ChunkUtils.key(chunk), count);
                }
            }
        }
    }

    private int countObserversInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (chunk.getBlock(x, y, z).getType() == Material.OBSERVER) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
