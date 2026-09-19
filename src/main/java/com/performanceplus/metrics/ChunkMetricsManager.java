package com.performanceplus.metrics;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.ChunkUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkMetricsManager implements Listener {

    private final PerformancePlus plugin;
    private final Map<String, Metrics> metrics = new ConcurrentHashMap<>();

    public ChunkMetricsManager(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public Metrics get(Chunk chunk) {
        return metrics.computeIfAbsent(ChunkUtils.key(chunk), k -> new Metrics());
    }

    public int size() { return metrics.size(); }

    public Metrics getIfPresent(Chunk chunk) {
        return metrics.get(ChunkUtils.key(chunk));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!plugin.getConfigManager().isWorldIgnored(event.getWorld())) {
            get(event.getChunk());
            initializeBlocks(event.getChunk());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        metrics.remove(ChunkUtils.key(event.getChunk()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (plugin.getConfigManager().isWorldIgnored(event.getChunk().getWorld())) {
            return;
        }
        Metrics m = get(event.getChunk());
        for (Entity entity : event.getEntities()) {
            addEntity(m, entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        Metrics m = getIfPresent(event.getChunk());
        if (m == null) return;
        for (Entity entity : event.getEntities()) {
            removeEntity(m, entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.PLAYER
                || plugin.getConfigManager().isWorldIgnored(event.getLocation().getWorld())) {
            return;
        }
        addEntity(get(event.getLocation().getChunk()), event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER || entity.getLocation().getWorld() == null) {
            return;
        }
        Metrics m = getIfPresent(entity.getLocation().getChunk());
        if (m != null) removeEntity(m, entity);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.PLAYER || event.getFrom().getWorld() == null || event.getTo() == null
                || event.getTo().getWorld() == null) {
            return;
        }

        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (ChunkUtils.key(from).equals(ChunkUtils.key(to))) return;

        Metrics source = getIfPresent(from);
        if (source != null) removeEntity(source, entity);
        if (!plugin.getConfigManager().isWorldIgnored(to.getWorld())) {
            addEntity(get(to), entity);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        adjustBlock(event.getBlock().getChunk(), event.getBlock().getType(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        adjustBlock(event.getBlock().getChunk(), event.getBlock().getType(), -1);
    }

    private void adjustBlock(Chunk chunk, Material material, int delta) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) return;
        Metrics m = get(chunk);
        switch (material) {
            case HOPPER -> m.hoppers = Math.max(0, m.hoppers + delta);
            case SPAWNER -> m.spawners = Math.max(0, m.spawners + delta);
            case PISTON, STICKY_PISTON -> m.pistons = Math.max(0, m.pistons + delta);
            case OBSERVER -> m.observers = Math.max(0, m.observers + delta);
            default -> { }
        }
    }

    public void initializeBlocks(Chunk chunk) {
        if (plugin.getConfigManager().isWorldIgnored(chunk.getWorld())) return;
        Metrics m = get(chunk);
        m.hoppers = 0;
        m.spawners = 0;
        m.pistons = 0;
        m.observers = 0;
        for (BlockState state : chunk.getTileEntities()) {
            switch (state.getType()) {
                case HOPPER -> m.hoppers++;
                case SPAWNER -> m.spawners++;
                case PISTON, STICKY_PISTON -> m.pistons++;
                case OBSERVER -> m.observers++;
                default -> { }
            }
        }
    }

    private void addEntity(Metrics m, Entity entity) {
        m.entities++;
        if (entity instanceof LivingEntity && !(entity instanceof Player)) m.mobs++;
        switch (entity.getType()) {
            case ITEM -> m.items++;
            case EXPERIENCE_ORB -> m.xpOrbs++;
            case HOPPER_MINECART -> m.hopperMinecarts++;
            default -> { }
        }
    }

    private void removeEntity(Metrics m, Entity entity) {
        m.entities = Math.max(0, m.entities - 1);
        if (entity instanceof LivingEntity && !(entity instanceof Player)) m.mobs = Math.max(0, m.mobs - 1);
        switch (entity.getType()) {
            case ITEM -> m.items = Math.max(0, m.items - 1);
            case EXPERIENCE_ORB -> m.xpOrbs = Math.max(0, m.xpOrbs - 1);
            case HOPPER_MINECART -> m.hopperMinecarts = Math.max(0, m.hopperMinecarts - 1);
            default -> { }
        }
    }

    public static final class Metrics {
        private int entities;
        private int mobs;
        private int items;
        private int xpOrbs;
        private int hopperMinecarts;
        private int hoppers;
        private int spawners;
        private int pistons;
        private int observers;

        public int entities() { return entities; }
        public int mobs() { return mobs; }
        public int items() { return items; }
        public int xpOrbs() { return xpOrbs; }
        public int hoppers() { return hoppers + hopperMinecarts; }
        public int spawners() { return spawners; }
        public int pistons() { return pistons; }
        public int observers() { return observers; }
    }
}
