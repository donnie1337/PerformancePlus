package com.performanceplus;

import com.performanceplus.commands.CleanupCommand;
import com.performanceplus.commands.LimitsCommand;
import com.performanceplus.commands.PerformancePlusCommand;
import com.performanceplus.config.ConfigManager;
import com.performanceplus.limiters.ChunkGenerationController;
import com.performanceplus.limiters.ComponentLimiter;
import com.performanceplus.limiters.EntityLimiter;
import com.performanceplus.limiters.FarmController;
import com.performanceplus.limiters.HopperLimiter;
import com.performanceplus.limiters.ItemCleanupManager;
import com.performanceplus.limiters.MobLimiter;
import com.performanceplus.limiters.ObserverController;
import com.performanceplus.limiters.PistonController;
import com.performanceplus.limiters.RedstoneLimiter;
import com.performanceplus.limiters.SpawnerLimiter;
import com.performanceplus.metrics.ChunkMetricsManager;
import com.performanceplus.monitor.PerformanceMonitor;
import com.performanceplus.util.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PerformancePlus extends JavaPlugin implements Listener {

    private static PerformancePlus instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private PerformanceMonitor performanceMonitor;
    private ChunkMetricsManager metricsManager;
    private FarmController farmController;
    private ItemCleanupManager itemCleanupManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        metricsManager = new ChunkMetricsManager(this);
        farmController = new FarmController(this);
        itemCleanupManager = new ItemCleanupManager(this);
        performanceMonitor = new PerformanceMonitor(this);

        registerListeners();
        for (org.bukkit.World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                metricsManager.initializeLoadedChunk(chunk);
            }
        }
        registerCommands();
        itemCleanupManager.start();
        performanceMonitor.start();

        getLogger().info(messageManager.get("plugin.habilitado"));
    }

    @Override
    public void onDisable() {
        if (performanceMonitor != null) performanceMonitor.stop();
        if (itemCleanupManager != null) itemCleanupManager.stop();
        if (messageManager != null) getLogger().info(messageManager.get("plugin.desabilitado"));
        instance = null;
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(metricsManager, this);
        pm.registerEvents(new MobLimiter(this), this);
        pm.registerEvents(new SpawnerLimiter(this), this);
        pm.registerEvents(new EntityLimiter(this), this);
        pm.registerEvents(new RedstoneLimiter(this), this);
        pm.registerEvents(new ComponentLimiter(this), this);
        pm.registerEvents(new HopperLimiter(this), this);
        pm.registerEvents(new PistonController(this), this);
        pm.registerEvents(new ObserverController(this), this);
        pm.registerEvents(new ChunkGenerationController(this), this);
        pm.registerEvents(farmController, this);
    }

    private void registerCommands() {
        PerformancePlusCommand commandExecutor = new PerformancePlusCommand(this);
        PluginCommand command = getCommand("performanceplus");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }

        PluginCommand cleanupCommand = getCommand("limpeza");
        if (cleanupCommand != null) {
            cleanupCommand.setExecutor(new CleanupCommand(this));
        }

        LimitsCommand limits = new LimitsCommand(this);
        PluginCommand limitsCommand = getCommand("limites");
        if (limitsCommand != null) {
            limitsCommand.setExecutor(limits);
            getServer().getPluginManager().registerEvents(limits, this);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        // Garante que os aliases públicos apareçam na lista de comandos do cliente.
        event.getCommands().add("limites");
        event.getCommands().add("limite");
    }

    public static PerformancePlus getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public ChunkMetricsManager getMetricsManager() { return metricsManager; }
    public FarmController getFarmController() { return farmController; }
    public ItemCleanupManager getItemCleanupManager() { return itemCleanupManager; }

    public int getPistonCount(Chunk chunk) {
        return metricsManager.get(chunk).pistons();
    }

    public int getObserverCount(Chunk chunk) {
        return metricsManager.get(chunk).observers();
    }
}
