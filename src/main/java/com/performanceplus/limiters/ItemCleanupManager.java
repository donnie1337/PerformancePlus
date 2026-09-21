package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemCleanupManager {
    private final PerformancePlus plugin;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public ItemCleanupManager(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reload();
    }

    public void reload() {
        stop();
        if (!plugin.getConfig().getBoolean("lixeiro.habilitado", true)) return;

        long interval = Math.max(1L, plugin.getConfig().getLong("lixeiro.intervalo-minutos", 30L)) * 60L * 20L;
        scheduleRepeating("lixeiro.cinco-minutos", interval - 5L * 60L * 20L, interval, false);
        scheduleRepeating("lixeiro.um-minuto", interval - 60L * 20L, interval, false);
        scheduleRepeating("lixeiro.contagem", interval - 4L * 20L, interval, true);
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::clean, interval, interval));
    }

    public void stop() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private void scheduleRepeating(String message, long delay, long interval, boolean countdown) {
        if (delay <= 0L) return;
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdown) {
                startCountdown();
            } else {
                broadcast(message, Map.of());
            }
        }, delay, interval));
    }

    private void startCountdown() {
        final int[] second = {4};
        final BukkitTask[] task = new BukkitTask[1];
        task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            broadcast("lixeiro.contagem", Map.of("{segundo}", second[0] + (second[0] == 1 ? " segundo" : " segundos")));
            playForEveryone(Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
            if (--second[0] == 0) task[0].cancel();
        }, 0L, 20L);
        tasks.add(task[0]);
    }

    public int cleanNow() {
        return clean();
    }

    private int clean() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            if (plugin.getConfigManager().isWorldIgnored(world)) continue;
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.isValid()) {
                    item.remove();
                    removed++;
                }
            }
        }

        broadcast("lixeiro.limpo", Map.of("{valor}", String.valueOf(removed)));
        playForEveryone(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
        return removed;
    }

    private void broadcast(String path, Map<String, String> placeholders) {
        Bukkit.broadcastMessage(plugin.getMessageManager().get(path, placeholders));
    }

    private void playForEveryone(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
