package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CleanupCommand implements CommandExecutor {
    private final PerformancePlus plugin;

    public CleanupCommand(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("performanceplus.admin")) {
            plugin.getMessageManager().send(sender, "comandos.sem-permissao");
            return true;
        }

        plugin.getItemCleanupManager().cleanNow();
        return true;
    }
}
