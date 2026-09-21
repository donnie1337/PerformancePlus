package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentLimiter implements Listener {
    private static final Map<Material, String> BLOCK_LIMITS = Map.ofEntries(
            Map.entry(Material.PISTON, "pistoes"),
            Map.entry(Material.STICKY_PISTON, "pistoes-com-slime"),
            Map.entry(Material.COMPARATOR, "comparadores"),
            Map.entry(Material.DISPENSER, "ejetores"),
            Map.entry(Material.DROPPER, "liberadores"),
            Map.entry(Material.REDSTONE_BLOCK, "blocos-de-redstone"),
            Map.entry(Material.REDSTONE_LAMP, "lampadas-de-redstone"),
            Map.entry(Material.REDSTONE_TORCH, "tochas-de-redstone"),
            Map.entry(Material.REDSTONE_WALL_TORCH, "tochas-de-redstone"),
            Map.entry(Material.REDSTONE_WIRE, "po-de-redstone"),
            Map.entry(Material.REPEATER, "repetidores"),
            Map.entry(Material.SCULK_SENSOR, "sensores-de-sculk"),
            Map.entry(Material.CALIBRATED_SCULK_SENSOR, "sensores-de-sculk"),
            Map.entry(Material.SCULK_CATALYST, "catalisadores-de-sculk"),
            Map.entry(Material.SCULK_SHRIEKER, "emissores-de-sculk"),
            Map.entry(Material.BEACON, "sinalizadores")
    );

    private static final Map<Material, EntityType> ENTITY_ITEMS = Map.of(
            Material.ARMOR_STAND, EntityType.ARMOR_STAND,
            Material.MINECART, EntityType.MINECART,
            Material.CHEST_MINECART, EntityType.CHEST_MINECART,
            Material.FURNACE_MINECART, EntityType.FURNACE_MINECART,
            Material.TNT_MINECART, EntityType.TNT_MINECART
    );

    private static final Map<EntityType, String> ENTITY_LIMITS = Map.of(
            EntityType.ARMOR_STAND, "suporte-armaduras",
            EntityType.MINECART, "carrinho-de-mina",
            EntityType.CHEST_MINECART, "carrinho-com-bau",
            EntityType.FURNACE_MINECART, "carrinho-com-fornalha",
            EntityType.TNT_MINECART, "carrinho-com-dinamite"
    );

    private final PerformancePlus plugin;
    private final Map<String, Map<String, Integer>> blockCounts = new ConcurrentHashMap<>();

    public ComponentLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String key = BLOCK_LIMITS.get(event.getBlock().getType());
        if (key == null) return;

        Chunk chunk = event.getBlock().getChunk();
        if (event.getPlayer().hasPermission("performanceplus.bypass.componentes") || !isEnabled(chunk, key)) return;

        int limit = limit(chunk, key);
        int existing = blockCountBeforePlacement(event, key);
        if (limit > 0 && existing >= limit) {
            event.setCancelled(true);
            sendLimit(event.getPlayer(), limit, displayName(key));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void updateAfterPlace(BlockPlaceEvent event) {
        String key = BLOCK_LIMITS.get(event.getBlock().getType());
        if (key == null) return;

        String chunkKey = ChunkUtils.key(event.getBlock().getChunk());
        if (event.isCancelled()) {
            blockCounts.remove(chunkKey);
            return;
        }

        Map<String, Integer> counts = blockCounts.get(chunkKey);
        if (counts != null) counts.merge(key, 1, Integer::sum);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void updateAfterBreak(BlockBreakEvent event) {
        String key = BLOCK_LIMITS.get(event.getBlock().getType());
        if (key == null) return;

        String chunkKey = ChunkUtils.key(event.getBlock().getChunk());
        if (event.isCancelled()) {
            blockCounts.remove(chunkKey);
            return;
        }

        Map<String, Integer> counts = blockCounts.get(chunkKey);
        if (counts != null) counts.computeIfPresent(key, (ignored, count) -> Math.max(0, count - 1));
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        blockCounts.remove(ChunkUtils.key(event.getChunk()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityItemUse(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getClickedBlock() == null) return;

        EntityType type = ENTITY_ITEMS.get(event.getItem().getType());
        if (type == null) return;

        String key = ENTITY_LIMITS.get(type);
        Chunk chunk = event.getClickedBlock().getChunk();
        if (event.getPlayer().hasPermission("performanceplus.bypass.componentes") || !isEnabled(chunk, key)) return;

        int limit = limit(chunk, key);
        if (limit > 0 && entityCount(chunk, type) >= limit) {
            event.setCancelled(true);
            sendLimit(event.getPlayer(), limit, displayName(key));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleCreate(VehicleCreateEvent event) {
        String key = ENTITY_LIMITS.get(event.getVehicle().getType());
        if (key == null) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        if (!isEnabled(chunk, key)) return;

        int limit = limit(chunk, key);
        if (limit > 0 && entityCount(chunk, event.getVehicle().getType()) >= limit) {
            event.setCancelled(true);
        }
    }

    private boolean isEnabled(Chunk chunk, String key) {
        return !plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                && plugin.getConfigManager().isLimitEnabled(chunk.getWorld(), key);
    }

    private int limit(Chunk chunk, String key) {
        return plugin.getConfigManager().getFixedLimit(chunk.getWorld(), key, 0);
    }

    private int blockCountBeforePlacement(BlockPlaceEvent event, String key) {
        Chunk chunk = event.getBlock().getChunk();
        String chunkKey = ChunkUtils.key(chunk);
        Map<String, Integer> counts = blockCounts.computeIfAbsent(chunkKey, ignored -> scan(chunk, event));
        return counts.getOrDefault(key, 0);
    }

    private Map<String, Integer> scan(Chunk chunk, BlockPlaceEvent pendingPlacement) {
        Map<String, Integer> counts = new HashMap<>();
        Block pendingBlock = pendingPlacement.getBlock();
        Material replacedMaterial = pendingPlacement.getBlockReplacedState().getType();
        int minY = chunk.getWorld().getMinHeight();
        int maxY = chunk.getWorld().getMaxHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Material material = chunk.getBlock(x, y, z).getType();
                    if (x == pendingBlock.getX() && y == pendingBlock.getY() && z == pendingBlock.getZ()) {
                        material = replacedMaterial;
                    }
                    String key = BLOCK_LIMITS.get(material);
                    if (key != null) counts.merge(key, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private int entityCount(Chunk chunk, EntityType type) {
        int total = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == type) total++;
        }
        return total;
    }

    private void sendLimit(Player player, int limit, String item) {
        plugin.getMessageManager().send(player, "limites.componente",
                Map.of("{limite}", String.valueOf(limit), "{item}", item));
    }

    private String displayName(String key) {
        return switch (key) {
            case "suporte-armaduras" -> "Suportes de armaduras";
            case "carrinho-com-bau" -> "Carrinhos com baú";
            case "carrinho-com-fornalha" -> "Carrinhos com fornalha";
            case "carrinho-de-mina" -> "Carrinhos de mina";
            case "carrinho-com-dinamite" -> "Carrinhos com dinamite";
            case "pistoes" -> "Pistões";
            case "pistoes-com-slime" -> "Pistões com slime";
            case "comparadores" -> "Comparadores";
            case "ejetores" -> "Ejetores";
            case "liberadores" -> "Liberadores";
            case "blocos-de-redstone" -> "Blocos de redstone";
            case "lampadas-de-redstone" -> "Lâmpadas de redstone";
            case "tochas-de-redstone" -> "Tochas de redstone";
            case "po-de-redstone" -> "Pó de redstone";
            case "repetidores" -> "Repetidores";
            case "sensores-de-sculk" -> "Sensores de sculk";
            case "catalisadores-de-sculk" -> "Catalisadores de sculk";
            case "emissores-de-sculk" -> "Emissores de sculk";
            case "sinalizadores" -> "Sinalizadores";
            default -> key;
        };
    }
}
