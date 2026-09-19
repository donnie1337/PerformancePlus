package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.CooldownTracker;
import com.performanceplus.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * IMPORTANTE: a API do Spigot/Bukkit não permite cancelar a geração de um
 * chunk — quando ChunkLoadEvent dispara com isNewChunk() = true, o chunk já
 * foi totalmente gerado. Por isso, este módulo funciona como um monitor de
 * alerta (detecta geração acelerada e avisa a staff), e não como um bloqueio
 * "duro". Para limitar geração de fato em tempo real, use uma ferramenta
 * dedicada (ex: Chunky para pré-gerar o mundo) ou, no caso do Paper/Folia,
 * hooks assíncronos de geração de chunk via NMS.
 */
public class ChunkGenerationController implements Listener {

    private final PerformancePlus plugin;
    private final CooldownTracker globalTracker = new CooldownTracker(1000L);
    private static final String GLOBAL_KEY = "global-chunkgen";

    public ChunkGenerationController(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return;
        }
        if (plugin.getConfigManager().isWorldIgnored(event.getWorld())) {
            return;
        }

        int limit = plugin.getConfigManager().getGlobalInt("limits.max-chunks-generated-per-second", 10);
        if (limit <= 0) {
            return;
        }

        int count = globalTracker.registerAndCount(GLOBAL_KEY);

        // Dispara o alerta só uma vez por "estouro" de janela, para não
        // floodar console e chat quando alguém está voando por muito tempo.
        if (count == limit + 1) {
            plugin.getLogger().warning("Geração de chunks acima do limite configurado ("
                    + limit + "/s)! Isso pode causar lag ao explorar o mundo.");
            notifyStaff();
        }
    }

    private void notifyStaff() {
        if (!plugin.getConfigManager().getGlobalBoolean("monitoring.broadcast-to-ops", true)) {
            return;
        }

        String msg = plugin.getConfigManager().getPrefix()
                + "&eGeração de terreno acima do limite recomendado. Considere pré-gerar o mundo.";

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("performanceplus.notify")) {
                p.sendMessage(MessageUtil.color(msg));
            }
        }
    }
}
