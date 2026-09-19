package com.performanceplus.config;

import com.performanceplus.PerformancePlus;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Centraliza a leitura do config.yml, incluindo a lógica de overrides
 * por mundo: qualquer chave definida em "worlds.<mundo>.limits.<chave>"
 * tem prioridade sobre o valor equivalente em "limits.<chave>".
 */
public class ConfigManager {

    private final PerformancePlus plugin;

    public ConfigManager(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration raw() {
        return plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    /**
     * Busca um limite inteiro, priorizando o valor específico do mundo
     * (worlds.<mundo>.limits.<key>) e caindo para o valor global
     * (limits.<key>) quando não houver override.
     */
    public int getLimit(World world, String key, int def) {
        FileConfiguration cfg = raw();
        String worldPath = "worlds." + world.getName() + ".limits." + key;
        if (cfg.contains(worldPath)) {
            return cfg.getInt(worldPath, def);
        }
        return cfg.getInt("limits." + key, def);
    }

    public double getLimitDouble(World world, String key, double def) {
        FileConfiguration cfg = raw();
        String worldPath = "worlds." + world.getName() + ".limits." + key;
        if (cfg.contains(worldPath)) {
            return cfg.getDouble(worldPath, def);
        }
        return cfg.getDouble("limits." + key, def);
    }

    public int getGlobalInt(String path, int def) {
        return raw().getInt(path, def);
    }

    public double getGlobalDouble(String path, double def) {
        return raw().getDouble(path, def);
    }

    public boolean getGlobalBoolean(String path, boolean def) {
        return raw().getBoolean(path, def);
    }

    public String getPrefix() {
        return raw().getString("settings.language-prefix", "&8[&bPerformancePlus&8] &7");
    }

    public boolean isWorldIgnored(World world) {
        List<String> ignored = raw().getStringList("settings.ignored-worlds");
        return ignored.contains(world.getName());
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
