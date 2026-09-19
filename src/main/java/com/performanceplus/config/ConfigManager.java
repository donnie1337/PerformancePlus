package com.performanceplus.config;

import com.performanceplus.PerformancePlus;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    private final PerformancePlus plugin;

    public ConfigManager(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration raw() { return plugin.getConfig(); }

    public void reload() { plugin.reloadConfig(); }

    public int getLimit(World world, String key, int def) {
        String path = "limites." + key;
        String worldPath = "mundos." + world.getName() + ".limites." + key;
        FileConfiguration cfg = raw();
        return cfg.contains(worldPath) ? cfg.getInt(worldPath, def) : cfg.getInt(path, def);
    }

    public double getDouble(World world, String key, double def) {
        String path = "performance." + key;
        String worldPath = "mundos." + world.getName() + ".performance." + key;
        FileConfiguration cfg = raw();
        return cfg.contains(worldPath) ? cfg.getDouble(worldPath, def) : cfg.getDouble(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return raw().getBoolean(path, def);
    }

    public int getInt(String path, int def) {
        return raw().getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return raw().getDouble(path, def);
    }

    public boolean isWorldIgnored(World world) {
        return raw().getStringList("mundos.ignorados").contains(world.getName());
    }

    public boolean isEnabled(String path, boolean def) {
        return raw().getBoolean(path, def);
    }

    public String getPrefix() {
        return raw().getString("configuracao.prefixo", "&8[&bPerformancePlus&8] &7");
    }

    public boolean isLimitEnabled(String key) {
        return raw().getBoolean("limites." + key + ".habilitado", true);
    }

    public List<String> getIgnoredWorlds() {
        return raw().getStringList("mundos.ignorados");
    }

    public ConfigurationSection getWorldSection(World world) {
        return raw().getConfigurationSection("mundos." + world.getName());
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
