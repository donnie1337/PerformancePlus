package com.performanceplus;

import com.performanceplus.commands.PerformancePlusCommand;
import com.performanceplus.config.ConfigManager;
import com.performanceplus.limiters.ChunkGenerationController;
import com.performanceplus.limiters.EntityLimiter;
import com.performanceplus.limiters.FarmController;
import com.performanceplus.limiters.HopperLimiter;
import com.performanceplus.limiters.ItemLimiter;
import com.performanceplus.limiters.MobLimiter;
import com.performanceplus.limiters.ObserverController;
import com.performanceplus.limiters.PistonController;
import com.performanceplus.limiters.RedstoneLimiter;
import com.performanceplus.limiters.SpawnerLimiter;
import com.performanceplus.limiters.XPLimiter;
import com.performanceplus.monitor.PerformanceMonitor;
import com.performanceplus.tasks.ChunkScanTask;
import org.bukkit.Chunk;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class PerformancePlus extends JavaPlugin {

    private static PerformancePlus instance;

    private ConfigManager configManager;
    private PerformanceMonitor performanceMonitor;
    private FarmController farmController;
    private PistonController pistonController;
    private ObserverController observerController;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.farmController = new FarmController(this);
        this.pistonController = new PistonController(this);
        this.observerController = new ObserverController(this);

        registerListeners();
        registerCommands();
        startTasks();

        getLogger().info("PerformancePlus habilitado! Monitorando a performance do servidor...");
    }

    @Override
    public void onDisable() {
        if (performanceMonitor != null) {
            performanceMonitor.stop();
        }
        getLogger().info("PerformancePlus desabilitado.");
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MobLimiter(this), this);
        pm.registerEvents(new SpawnerLimiter(this), this);
        pm.registerEvents(new EntityLimiter(this), this);
        pm.registerEvents(new ItemLimiter(this), this);
        pm.registerEvents(new XPLimiter(this), this);
        pm.registerEvents(new RedstoneLimiter(this), this);
        pm.registerEvents(new HopperLimiter(this), this);
        pm.registerEvents(pistonController, this);
        pm.registerEvents(observerController, this);
        pm.registerEvents(new ChunkGenerationController(this), this);
    }

    private void registerCommands() {
        PerformancePlusCommand cmd = new PerformancePlusCommand(this);
        PluginCommand command = getCommand("performanceplus");
        if (command != null) {
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        } else {
            getLogger().warning("Não foi possível registrar o comando 'performanceplus'. Verifique o plugin.yml.");
        }
    }

    private void startTasks() {
        this.performanceMonitor = new PerformanceMonitor(this);
        performanceMonitor.start();

        int interval = configManager.getGlobalInt("settings.check-interval-ticks", 100);
        new ChunkScanTask(this, farmController).runTaskTimer(this, 100L, Math.max(20, interval));
    }

    public static PerformancePlus getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public FarmController getFarmController() {
        return farmController;
    }

    public PistonController getPistonController() {
        return pistonController;
    }

    public ObserverController getObserverController() {
        return observerController;
    }

    public int getPistonCount(Chunk chunk) {
        return pistonController.getCount(chunk);
    }

    public int getObserverCount(Chunk chunk) {
        return observerController.getCount(chunk);
    }
}
