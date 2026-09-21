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
        int value = getRawLimit(world, key, def);
        // Hoppers e spawners permanecem fixos por chunk.
        if ("hoppers".equalsIgnoreCase(key) || "spawners".equalsIgnoreCase(key)) return value;
        return applyAdaptiveLimit(value);
    }

    /**
     * Retorna o limite de uma criatura. Se não houver valor específico, usa
     * o limite padrão de mobs definido em limites.mobs.valor.
     */
    public int getMobLimit(World world, String mobKey, int def) {
        FileConfiguration cfg = raw();
        String base = "limites.mobs.criaturas." + mobKey + ".valor";
        String worldBase = "mundos." + world.getName() + ".limites.mobs.criaturas." + mobKey + ".valor";
        int value = cfg.contains(worldBase) ? cfg.getInt(worldBase, def)
                : (cfg.contains(base) ? cfg.getInt(base, def) : getRawLimit(world, "mobs", def));
        return applyAdaptiveLimit(value);
    }

    /**
     * Retorna o raio, em blocos, usado para contar a mesma criatura perto
     * do ponto de nascimento.
     */
    public double getMobRadius(World world, String mobKey, double def) {
        FileConfiguration cfg = raw();
        String base = "limites.mobs.criaturas." + mobKey + ".raio";
        String worldBase = "mundos." + world.getName() + ".limites.mobs.criaturas." + mobKey + ".raio";
        String defaultPath = "limites.mobs.raio";
        double radius = cfg.contains(worldBase) ? cfg.getDouble(worldBase, def)
                : (cfg.contains(base) ? cfg.getDouble(base, def) : cfg.getDouble(defaultPath, def));
        return Math.max(1.0, radius);
    }

    private int getRawLimit(World world, String key, int def) {
        String path = "limites." + key + ".valor";
        String worldPath = "mundos." + world.getName() + ".limites." + key;
        String worldValuePath = worldPath + ".valor";
        FileConfiguration cfg = raw();
        return cfg.contains(worldValuePath) ? cfg.getInt(worldValuePath, def)
                : (cfg.contains(worldPath) ? cfg.getInt(worldPath, def) : cfg.getInt(path, def));
    }

    private int applyAdaptiveLimit(int value) {
        FileConfiguration cfg = raw();
        if (value <= 0 || !cfg.getBoolean("performance.protecao-adaptativa.habilitado", true)
                || plugin.getPerformanceMonitor() == null) return value;
        return Math.max(1, (int) Math.floor(value * plugin.getPerformanceMonitor().getLimitMultiplier()));
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

    public boolean isLimitEnabled(World world, String key) {
        String worldPath = "mundos." + world.getName() + ".limites." + key + ".habilitado";
        return raw().contains(worldPath) ? raw().getBoolean(worldPath) : isLimitEnabled(key);
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
