package com.performanceplus.util;

import com.performanceplus.PerformancePlus;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

public final class MessageManager {
    private final PerformancePlus plugin;
    private FileConfiguration messages;

    public MessageManager(PerformancePlus plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "mensagens.yml");
        if (!file.exists()) {
            plugin.saveResource("mensagens.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path) {
        return color(messages.getString(path, path));
    }

    public String get(String path, Map<String, String> placeholders) {
        String message = messages.getString(path, path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return color(message);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    public String prefix() {
        return get("prefixo");
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
