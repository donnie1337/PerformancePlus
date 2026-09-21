package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.metrics.ChunkMetricsManager.Metrics;
import com.performanceplus.util.MessageManager;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class PerformancePlusCommand implements CommandExecutor, TabCompleter {
    private final PerformancePlus plugin;
    private static final List<String> SUBCOMMANDS = List.of("status", "chunk", "farms", "reload", "help");

    public PerformancePlusCommand(PerformancePlus plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager messages = plugin.getMessageManager();
        if (args.length == 0) { sendStatus(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "status" -> sendStatus(sender);
            case "chunk" -> sendChunkInfo(sender);
            case "farms" -> sendFarms(sender);
            case "reload" -> {
                if (!sender.hasPermission("performanceplus.admin")) {
                    messages.send(sender, "comandos.sem-permissao");
                    return true;
                }
                plugin.getConfigManager().reload();
                messages.reload();
                plugin.getItemCleanupManager().reload();
                messages.send(sender, "comandos.config-recarregada");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        MessageManager m = plugin.getMessageManager();
        m.send(sender, "comandos.status.titulo");
        m.send(sender, "comandos.status.tps", Map.of("{tps}", String.format("%.2f", plugin.getPerformanceMonitor().getTps())));
        m.send(sender, "comandos.status.mspt", Map.of("{mspt}", String.format("%.2f", plugin.getPerformanceMonitor().getMspt())));
        m.send(sender, "comandos.status.nivel", Map.of("{nivel}", String.valueOf(plugin.getPerformanceMonitor().getProtectionLevel())));
        m.send(sender, "comandos.status.chunks-monitorados", Map.of("{valor}", String.valueOf(plugin.getMetricsManager().size())));
        m.send(sender, "comandos.status.chunks-sinalizados", Map.of("{valor}", String.valueOf(plugin.getFarmController().getFlaggedChunks().size())));
    }

    public void sendChunkInfo(CommandSender sender) {
        MessageManager m = plugin.getMessageManager();
        if (!(sender instanceof Player player)) {
            m.send(sender, "comandos.apenas-jogador");
            return;
        }
        Chunk chunk = player.getLocation().getChunk();
        Metrics metrics = plugin.getMetricsManager().get(chunk);
        m.send(sender, "comandos.chunk.titulo");
        m.send(sender, "comandos.chunk.cabecalho");
        m.send(sender, "comandos.chunk.coordenadas", Map.of(
                "{x}", String.valueOf(chunk.getX()),
                "{z}", String.valueOf(chunk.getZ())));
        m.send(sender, "comandos.chunk.mobs", Map.of("{valor}", String.valueOf(metrics.mobs())));
        m.send(sender, "comandos.chunk.entidades", Map.of("{valor}", String.valueOf(metrics.entities())));
        m.send(sender, "comandos.chunk.itens", Map.of("{valor}", String.valueOf(metrics.items())));
        m.send(sender, "comandos.chunk.xp", Map.of("{valor}", String.valueOf(metrics.xpOrbs())));
        m.send(sender, "comandos.chunk.hoppers", Map.of("{valor}", String.valueOf(metrics.hoppers())));
        m.send(sender, "comandos.chunk.spawners", Map.of("{valor}", String.valueOf(metrics.spawners())));
        m.send(sender, "comandos.chunk.pistoes", Map.of("{valor}", String.valueOf(metrics.pistons())));
        m.send(sender, "comandos.chunk.observers", Map.of("{valor}", String.valueOf(metrics.observers())));
        m.send(sender, "comandos.chunk.rodape");
    }

    private void sendFarms(CommandSender sender) {
        MessageManager m = plugin.getMessageManager();
        if (plugin.getFarmController().getFlaggedChunks().isEmpty()) {
            m.send(sender, "comandos.farms.nenhum");
            return;
        }
        m.send(sender, "comandos.farms.titulo");
        for (String key : plugin.getFarmController().getFlaggedChunks()) {
            m.send(sender, "comandos.farms.item", Map.of("{valor}", key));
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageManager m = plugin.getMessageManager();
        m.send(sender, "comandos.ajuda.status");
        m.send(sender, "comandos.ajuda.chunk");
        m.send(sender, "comandos.ajuda.farms");
        m.send(sender, "comandos.ajuda.reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
    }
}
