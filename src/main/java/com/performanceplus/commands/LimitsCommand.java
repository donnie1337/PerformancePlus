package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.Locale;

public class LimitsCommand implements CommandExecutor, Listener {

    private static final String TITLE = "§8§lLimites do servidor";
    private final PerformancePlus plugin;

    private static final LimitEntry[] LIMITS = {
            new LimitEntry("mobs", "Mobs por chunk", Material.ZOMBIE_HEAD),
            new LimitEntry("spawners", "Spawners por chunk", Material.SPAWNER),
            new LimitEntry("entidades", "Entidades por chunk", Material.ARMOR_STAND),
            new LimitEntry("itens", "Itens por chunk", Material.DIAMOND),
            new LimitEntry("xp-orbes", "Orbes de XP por chunk", Material.EXPERIENCE_BOTTLE),
            new LimitEntry("hoppers", "Hoppers por chunk", Material.HOPPER),
            new LimitEntry("redstone", "Redstone por chunk/tick", Material.REDSTONE),
            new LimitEntry("pistoes", "Pistões por chunk", Material.PISTON),
            new LimitEntry("pistoes-ativacoes-por-segundo", "Ativações de pistão/segundo", Material.PISTON),
            new LimitEntry("observers", "Observers por chunk", Material.OBSERVER),
            new LimitEntry("chunks-gerados-por-segundo", "Chunks gerados/segundo", Material.MAP)
    };

    public LimitsCommand(PerformancePlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigManager.color(plugin.getConfigManager().getPrefix()
                    + "&cEste comando só pode ser usado por jogadores."));
            return true;
        }

        open(player);
        return true;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        World world = player.getWorld();

        for (int i = 0; i < LIMITS.length; i++) {
            LimitEntry entry = LIMITS[i];
            int value = plugin.getConfigManager().getLimit(world, entry.key, 0);
            boolean override = plugin.getConfigManager().raw()
                    .contains("mundos." + world.getName() + ".limites." + entry.key);

            List<String> lore = new ArrayList<>();
            lore.add("§7Valor atual: §f" + formatValue(value));
            lore.add("§7Mundo: §f" + world.getName());
            lore.add(override ? "§eValor específico deste mundo"
                    : "§8Usando o limite global");
            lore.add("");
            lore.add("§8• §7Controlado pelo §fconfig.yml");

            ItemStack item = new ItemStack(entry.material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b§l" + entry.displayName);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slotFor(i), item);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§f§lInformações");
            infoMeta.setLore(List.of(
                    "§7Estes são os limites ativos no seu mundo.",
                    "§7Limites específicos do mundo têm prioridade",
                    "§7sobre os valores globais do config.yml.",
                    "",
                    "§8• §7Comando: §f/limites"
            ));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(22, info);

        player.openInventory(inventory);
    }

    private int slotFor(int index) {
        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        return slots[index];
    }

    private String formatValue(int value) {
        return value <= 0 ? "Desativado" : String.valueOf(value);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (TITLE.equals(event.getView().getTitle())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }

    private record LimitEntry(String key, String displayName, Material material) {
    }
}
