package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PerformancePlusCommand implements CommandExecutor, TabCompleter {
    private final PerformancePlus plugin;
    private static final List<String> SUBCOMMANDS = List.of("status", "chunk", "farms", "reload", "help");

    public PerformancePlusCommand(PerformancePlus plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        if (args.length == 0) { sendStatus(sender, prefix); return true; }

        switch (args[0].toLowerCase()) {
            case "status" -> sendStatus(sender, prefix);
            case "chunk" -> sendChunkInfo(sender, prefix);
            case "farms" -> sendFarms(sender, prefix);
            case "reload" -> {
                if (!sender.hasPermission("performanceplus.admin")) {
                    MessageUtil.send(sender, prefix, "&cVocê não tem permissão para isso.");
                    return true;
                }
                plugin.getConfigManager().reload();
                MessageUtil.send(sender, prefix, "&aConfiguração recarregada com sucesso.");
            }
            default -> sendHelp(sender, prefix);
        }
        return true;
    }

    private void sendStatus(CommandSender sender, String prefix) {
        MessageUtil.send(sender, prefix, "&f--- &bPerformancePlus &f---");
        MessageUtil.send(sender, prefix, "&7TPS: &f" + String.format("%.2f", plugin.getPerformanceMonitor().getTps()));
        MessageUtil.send(sender, prefix, "&7MSPT: &f" + String.format("%.2f", plugin.getPerformanceMonitor().getMspt()));
        MessageUtil.send(sender, prefix, "&7Nível de proteção: &f" + plugin.getPerformanceMonitor().getProtectionLevel());
        MessageUtil.send(sender, prefix, "&7Chunks monitorados: &f" + plugin.getMetricsManager().size());
        MessageUtil.send(sender, prefix, "&7Chunks sinalizados: &f" + plugin.getFarmController().getFlaggedChunks().size());
    }

    private void sendChunkInfo(CommandSender sender, String prefix) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, prefix, "&cEste comando só pode ser usado por jogadores.");
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        Metrics m = plugin.getMetricsManager().get(chunk);
        MessageUtil.send(sender, prefix, "&f--- &bChunk &f(" + chunk.getX() + ", " + chunk.getZ() + ") &f---");
        MessageUtil.send(sender, prefix, "&7Mobs: &f" + m.mobs());
        MessageUtil.send(sender, prefix, "&7Entidades: &f" + m.entities());
        MessageUtil.send(sender, prefix, "&7Itens: &f" + m.items());
        MessageUtil.send(sender, prefix, "&7XP: &f" + m.xpOrbs());
        MessageUtil.send(sender, prefix, "&7Hoppers: &f" + m.hoppers());
        MessageUtil.send(sender, prefix, "&7Spawners: &f" + m.spawners());
        MessageUtil.send(sender, prefix, "&7Pistões: &f" + m.pistons());
        MessageUtil.send(sender, prefix, "&7Observers: &f" + m.observers());
    }

    private void sendFarms(CommandSender sender, String prefix) {
        if (plugin.getFarmController().getFlaggedChunks().isEmpty()) {
            MessageUtil.send(sender, prefix, "&aNenhum chunk de alta concentração detectado.");
            return;
        }
        MessageUtil.send(sender, prefix, "&f--- &bChunks sinalizados &f---");
        for (String key : plugin.getFarmController().getFlaggedChunks()) {
            MessageUtil.send(sender, prefix, "&7• &f" + key);
        }
    }

    private void sendHelp(CommandSender sender, String prefix) {
        MessageUtil.send(sender, prefix, "&f/pperf status &7- Status de performance");
        MessageUtil.send(sender, prefix, "&f/pperf chunk &7- Limites e contadores do chunk atual");
        MessageUtil.send(sender, prefix, "&f/pperf farms &7- Chunks com alta concentração");
        MessageUtil.send(sender, prefix, "&f/pperf reload &7- Recarrega o config.yml");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
    }
}
