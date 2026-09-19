package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.limiters.HopperLimiter;
import com.performanceplus.limiters.SpawnerLimiter;
import com.performanceplus.util.MessageUtil;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PerformancePlusCommand implements CommandExecutor, TabCompleter {

    private final PerformancePlus plugin;
    private static final List<String> SUBCOMMANDS =
            Arrays.asList("reload", "status", "chunk", "farms", "recount", "help");

    public PerformancePlusCommand(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();

        if (args.length == 0) {
            sendStatus(sender, prefix);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("performanceplus.admin")) {
                    MessageUtil.send(sender, prefix, "&cVocê não tem permissão para isso.");
                    return true;
                }
                plugin.getConfigManager().reload();
                MessageUtil.send(sender, prefix, "&aConfiguração recarregada com sucesso!");
            }
            case "status" -> sendStatus(sender, prefix);
            case "chunk" -> sendChunkInfo(sender, prefix);
            case "farms" -> sendFarms(sender, prefix);
            case "recount" -> {
                if (!sender.hasPermission("performanceplus.admin")) {
                    MessageUtil.send(sender, prefix, "&cVocê não tem permissão para isso.");
                    return true;
                }
                MessageUtil.send(sender, prefix,
                        "&eRecontando pistões e observers nos chunks carregados... isso pode causar uma breve travada.");
                plugin.getPistonController().recountAll();
                plugin.getObserverController().recountAll();
                MessageUtil.send(sender, prefix, "&aRecontagem concluída!");
            }
            default -> sendHelp(sender, prefix);
        }
        return true;
    }

    private void sendStatus(CommandSender sender, String prefix) {
        double tps = plugin.getPerformanceMonitor().getTps();
        double mspt = plugin.getPerformanceMonitor().getMspt();

        MessageUtil.send(sender, prefix, "&f--- &bPerformancePlus &f---");
        MessageUtil.send(sender, prefix, "&7TPS: &f" + String.format("%.2f", tps));
        MessageUtil.send(sender, prefix, "&7MSPT: &f" + String.format("%.2f", mspt) + "ms");
        MessageUtil.send(sender, prefix, "&7Entidades carregadas: &f" + countAllEntities());
        MessageUtil.send(sender, prefix, "&7Chunks carregados: &f" + countLoadedChunks());
        MessageUtil.send(sender, prefix, "&7Farms sinalizadas: &f" + plugin.getFarmController().getFlaggedChunks().size());
    }

    private void sendChunkInfo(CommandSender sender, String prefix) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, prefix, "&cEste comando só pode ser usado por jogadores.");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        int mobs = 0;
        for (Entity e : chunk.getEntities()) {
            if (e instanceof LivingEntity && !(e instanceof Player)) {
                mobs++;
            }
        }

        MessageUtil.send(sender, prefix, "&f--- &bInfo do Chunk &f(" + chunk.getX() + ", " + chunk.getZ() + ") &f---");
        MessageUtil.send(sender, prefix, "&7Mobs: &f" + mobs);
        MessageUtil.send(sender, prefix, "&7Entidades totais: &f" + chunk.getEntities().length);
        MessageUtil.send(sender, prefix, "&7Spawners: &f" + SpawnerLimiter.countSpawners(chunk));
        MessageUtil.send(sender, prefix, "&7Hoppers: &f" + HopperLimiter.countHoppers(chunk));
        MessageUtil.send(sender, prefix, "&7Pistões: &f" + plugin.getPistonCount(chunk));
        MessageUtil.send(sender, prefix, "&7Observers: &f" + plugin.getObserverCount(chunk));
    }

    private void sendFarms(CommandSender sender, String prefix) {
        var farms = plugin.getFarmController().getFlaggedChunks();
        if (farms.isEmpty()) {
            MessageUtil.send(sender, prefix, "&aNenhuma farm suspeita detectada no momento.");
            return;
        }
        MessageUtil.send(sender, prefix, "&f--- &bFarms detectadas &f(" + farms.size() + ") &f---");
        for (String key : farms) {
            MessageUtil.send(sender, prefix, "&7- &f" + key);
        }
    }

    private void sendHelp(CommandSender sender, String prefix) {
        MessageUtil.send(sender, prefix, "&f/pperf status &7- Mostra TPS e status geral");
        MessageUtil.send(sender, prefix, "&f/pperf chunk &7- Mostra info do chunk onde você está");
        MessageUtil.send(sender, prefix, "&f/pperf farms &7- Lista chunks sinalizados como farm");
        MessageUtil.send(sender, prefix, "&f/pperf recount &7- Reconta pistões/observers (admin)");
        MessageUtil.send(sender, prefix, "&f/pperf reload &7- Recarrega a configuração (admin)");
    }

    private int countAllEntities() {
        int total = 0;
        for (var world : plugin.getServer().getWorlds()) {
            total += world.getEntities().size();
        }
        return total;
    }

    private int countLoadedChunks() {
        int total = 0;
        for (var world : plugin.getServer().getWorlds()) {
            total += world.getLoadedChunks().length;
        }
        return total;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
