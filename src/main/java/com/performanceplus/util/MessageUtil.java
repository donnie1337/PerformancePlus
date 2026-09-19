package com.performanceplus.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(CommandSender sender, String prefix, String message) {
        sender.sendMessage(color(prefix + message));
    }
}
