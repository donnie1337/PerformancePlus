package com.performanceplus.limiters;

import com.performanceplus.PerformancePlus;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.block.Action;

import java.util.Map;

public class HopperLimiter implements Listener {
    private final PerformancePlus plugin;

    public HopperLimiter(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Limita hoppers colocados como bloco.
     *
     * A contagem é conferida diretamente na chunk no momento da colocação,
     * usando apenas tile entities + entidades da chunk. Isso evita depender
     * exclusivamente do contador incremental, que pode ficar defasado em
     * situações de carregamento/descarregamento de chunks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.HOPPER) return;

        Chunk chunk = event.getBlock().getChunk();
        if (!limiteAtivo(chunk)) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.hoppers")) return;

        int limit = limite(chunk);
        if (limit <= 0) return;

        // A própria colocação ainda não precisa estar refletida no contador.
        // Se já existem 20, esta tentativa seria o 21º e deve ser cancelada.
        if (contarHoppers(chunk) >= limit) {
            event.setCancelled(true);
            enviarLimite(player, limit);
        }
    }

    /**
     * Bloqueia a tentativa de colocar um Hopper Minecart antes que o item
     * seja consumido. Assim, ao atingir o limite, o carrinho permanece no
     * inventário do jogador.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinecartInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.hasPermission("performanceplus.bypass.hoppers")) return;

        if (event.getItem() == null || event.getItem().getType() != Material.HOPPER_MINECART) {
            return;
        }

        Chunk chunk = event.getClickedBlock().getChunk();
        if (!limiteAtivo(chunk)) return;

        int limit = limite(chunk);
        if (limit <= 0) return;

        if (contarHoppers(chunk) >= limit) {
            event.setCancelled(true);
            enviarLimite(player, limit);
        }
    }

    /**
     * Proteção adicional para hopper minecarts criados por outros meios.
     * Para criação feita pelo jogador, o PlayerInteractEvent acima já bloqueia
     * antes do consumo do item.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMinecartCreate(VehicleCreateEvent event) {
        if (event.getVehicle().getType() != EntityType.HOPPER_MINECART) return;

        Chunk chunk = event.getVehicle().getLocation().getChunk();
        if (!limiteAtivo(chunk)) return;

        int limit = limite(chunk);
        if (limit <= 0) return;

        // A entidade que está sendo criada ainda pode não aparecer na lista
        // de entidades da chunk, portanto >= limit significa que ela seria
        // o próximo hopper e deve ser recusada.
        if (contarHoppers(chunk) >= limit) {
            event.setCancelled(true);
        }
    }

    private boolean limiteAtivo(Chunk chunk) {
        return !plugin.getConfigManager().isWorldIgnored(chunk.getWorld())
                && plugin.getConfigManager().isLimitEnabled(chunk.getWorld(), "hoppers");
    }

    private int limite(Chunk chunk) {
        return plugin.getConfigManager().getLimit(chunk.getWorld(), "hoppers", 8);
    }

    /**
     * Conta exatamente os dois tipos que participam do limite:
     * - HOPPER colocado no chão;
     * - HOPPER_MINECART dentro da mesma chunk.
     */
    private int contarHoppers(Chunk chunk) {
        int total = 0;

        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() == Material.HOPPER) {
                total++;
            }
        }

        for (Entity entity : chunk.getEntities()) {
            if (entity.getType() == EntityType.HOPPER_MINECART) {
                total++;
            }
        }

        return total;
    }

    private void enviarLimite(Player player, int limit) {
        plugin.getMessageManager().send(player, "limites.hoppers",
                Map.of("{limite}", String.valueOf(limit)));
    }
}
