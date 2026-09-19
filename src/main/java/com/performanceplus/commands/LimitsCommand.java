package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.config.ConfigManager;
import com.performanceplus.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LimitsCommand implements CommandExecutor, Listener {

    private final PerformancePlus plugin;

    private static final LimitEntry[] LIMITS = {
            new LimitEntry("mobs", "mobs", Material.ZOMBIE_HEAD),
            new LimitEntry("spawners", "spawners", Material.SPAWNER),
            new LimitEntry("entidades", "entidades", Material.ARMOR_STAND),
            new LimitEntry("itens", "itens", Material.DIAMOND),
            new LimitEntry("xp-orbes", "xp-orbes", Material.EXPERIENCE_BOTTLE),
            new LimitEntry("hoppers", "hoppers", Material.HOPPER),
            new LimitEntry("redstone", "redstone", Material.REDSTONE),
            new LimitEntry("pistoes", "pistoes", Material.PISTON),
            new LimitEntry("pistoes-ativacoes-por-segundo", "pistoes-ativacoes-por-segundo", Material.PISTON),
            new LimitEntry("observers", "observers", Material.OBSERVER),
            new LimitEntry("chunks-gerados-por-segundo", "chunks-gerados-por-segundo", Material.MAP)
    };

    public LimitsCommand(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().send(sender, "comandos.apenas-jogador");
            return true;
        }
        open(player);
        return true;
    }

    public void open(Player player) {
        MessageManager messages = plugin.getMessageManager();
        String title = messages.get("gui.titulo");
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        World world = player.getWorld();

        for (int i = 0; i < LIMITS.length; i++) {
            LimitEntry entry = LIMITS[i];
            int value = plugin.getConfigManager().getLimit(world, entry.key, 0);
            boolean override = plugin.getConfigManager().raw()
                    .contains("mundos." + world.getName() + ".limites." + entry.key);

            List<String> lore = new ArrayList<>();
            lore.add(messages.get("gui.lore.valor", Map.of("{valor}", formatValue(value))));
            lore.add(messages.get("gui.lore.mundo", Map.of("{mundo}", world.getName())));
            lore.add(messages.get(override ? "gui.lore.override" : "gui.lore.global"));
            lore.add("");
            lore.add(messages.get("gui.lore.controlado"));

            ItemStack item = new ItemStack(entry.material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b§l" + messages.get("gui.limite." + entry.messageKey));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(messages.get("gui.info.nome"));
            infoMeta.setLore(List.of(
                    messages.get("gui.info.linha1"),
                    messages.get("gui.info.linha2"),
                    messages.get("gui.info.linha3"),
                    "",
                    messages.get("gui.info.comando")
            ));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(22, info);
        player.openInventory(inventory);
    }

    private String formatValue(int value) {
        return value <= 0 ? "Desativado" : String.valueOf(value);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getMessageManager().get("gui.titulo").equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (plugin.getMessageManager().get("gui.titulo").equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private record LimitEntry(String key, String messageKey, Material material) {
    }
}
