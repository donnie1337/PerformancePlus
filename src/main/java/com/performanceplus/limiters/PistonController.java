package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import com.performanceplus.util.CooldownTracker;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla pistões de duas formas:
 *  1) Limite ESTÁTICO de pistões por chunk, mantido em um contador
 *     incremental (atualizado em BlockPlaceEvent/BlockBreakEvent). É barato,
 *     mas pode "desviar" se blocos forem colocados por vias que não passam
 *     por esses eventos (ex: WorldEdit). Use /pperf recount para corrigir.
 *  2) Limite de ATIVAÇÕES por segundo por chunk (extend/retract), que é o
 *     que realmente protege contra máquinas de pistão/flying machines
 *     causando lag por ativações repetidas.
 */
public class PistonController implements Listener {

    private final PerformancePlus plugin;
    private final CooldownTracker activityTracker = new CooldownTracker(1000L);
    private final ConcurrentHashMap<String, Integer> placedCount = new ConcurrentHashMap<>();
    private static final Set<Material> PISTONS = Set.of(Material.PISTON, Material.STICKY_PISTON);

    public PistonController(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!PISTONS.contains(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        Chunk chunk = block.getChunk();
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return;
        }
        if (player.hasPermission("performanceplus.bypass.pistons")) {
            return;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "pistons-per-chunk", 12);
        String key = ChunkUtils.key(chunk);
        int current = placedCount.getOrDefault(key, 0);

        if (current >= limit) {
            event.setCancelled(true);
            MessageUtil.send(player, plugin.getConfigManager().getPrefix(),
                    "&cLimite de &f" + limit + " &cpistão(ões) por chunk atingido!");
            return;
        }
        placedCount.merge(key, 1, Integer::sum);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!PISTONS.contains(block.getType())) {
            return;
        }

        String key = ChunkUtils.key(block.getChunk());
        placedCount.computeIfPresent(key, (k, v) -> Math.max(0, v - 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        if (isOverActivityLimit(event.getBlock().getChunk())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        if (isOverActivityLimit(event.getBlock().getChunk())) {
            event.setCancelled(true);
        }
    }

    private boolean isOverActivityLimit(Chunk chunk) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) {
            return false;
        }

        int limit = plugin.getConfigManager().getLimit(chunk.getWorld(), "piston-activations-per-second", 8);
        if (limit <= 0) {
            return false;
        }

        int count = activityTracker.registerAndCount(ChunkUtils.key(chunk));
        return count > limit;
    }

    public int getCount(Chunk chunk) {
        return placedCount.getOrDefault(ChunkUtils.key(chunk), 0);
    }

    /**
     * Refaz a contagem de pistões varrendo bloco a bloco todos os chunks
     * carregados. Operação pesada — use apenas sob demanda (comando
     * /pperf recount), nunca em um agendamento automático frequente.
     */
    public void recountAll() {
        placedCount.clear();
        for (World world : plugin.getServer().getWorlds()) {
            if (plugin.getConfigManager().isWorldIgnored(world)) {
                continue;
            }
            for (Chunk chunk : world.getLoadedChunks()) {
                int count = countPistonsInChunk(chunk);
                if (count > 0) {
                    placedCount.put(ChunkUtils.key(chunk), count);
                }
            }
        }
    }

    private int countPistonsInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    if (PISTONS.contains(chunk.getBlock(x, y, z).getType())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
