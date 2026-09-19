package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import com.performanceplus.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

public class LimitsCommand implements CommandExecutor, Listener {

    private static final String MAIN_TITLE = "&8&lLimites";
    private static final String REDSTONE_TITLE = "&8&lLimites &8• &cRedstone";
    private static final String CREATURES_TITLE = "&8&lLimites &8• &aCriaturas";
    private static final String GENERATORS_TITLE = "&8&lLimites &8• &5Geradores";
    private static final String DECORATIONS_TITLE = "&8&lLimites &8• &eDecorações";

    private final PerformancePlus plugin;

    private static final Category[] CATEGORIES = {
            new Category("redstone", "gui.categorias.redstone", Material.REDSTONE),
            new Category("criaturas", "gui.categorias.criaturas", Material.CREEPER_SPAWN_EGG),
            new Category("geradores", "gui.categorias.geradores", Material.SPAWNER),
            new Category("decoracoes", "gui.categorias.decoracoes", Material.ARMOR_STAND)
    };

    private static final CreatureCategory[] CREATURE_CATEGORIES = {
            new CreatureCategory("animais-terrestres", "gui.criaturas.animais-terrestres", Material.COW_SPAWN_EGG,
                    "cow", "sheep", "pig", "chicken", "horse", "donkey", "mule", "rabbit", "fox", "wolf"),
            new CreatureCategory("animais-aquaticos", "gui.criaturas.animais-aquaticos", Material.COD_SPAWN_EGG,
                    "cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin", "turtle", "axolotl", "guardian"),
            new CreatureCategory("monstros-comuns", "gui.criaturas.monstros-comuns", Material.ZOMBIE_SPAWN_EGG,
                    "zombie", "skeleton", "creeper", "spider", "cave_spider", "witch", "slime", "phantom", "enderman", "silverfish"),
            new CreatureCategory("nether", "gui.criaturas.nether", Material.BLAZE_SPAWN_EGG,
                    "blaze", "ghast", "magma_cube", "piglin", "piglin_brute", "zombified_piglin", "hoglin", "zoglin", "wither_skeleton", "strider"),
            new CreatureCategory("end", "gui.criaturas.end", Material.ENDERMAN_SPAWN_EGG,
                    "enderman", "shulker", "ender_dragon"),
            new CreatureCategory("pillagers", "gui.criaturas.pillagers", Material.PILLAGER_SPAWN_EGG,
                    "pillager", "vindicator", "evoker", "ravager", "illusioner"),
            new CreatureCategory("aldeoes-traders", "gui.criaturas.aldeoes-traders", Material.VILLAGER_SPAWN_EGG,
                    "villager", "wandering_trader"),
            new CreatureCategory("cavalos-especiais", "gui.criaturas.cavalos-especiais", Material.HORSE_SPAWN_EGG,
                    "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel"),
            new CreatureCategory("golems", "gui.criaturas.golems", Material.IRON_GOLEM_SPAWN_EGG,
                    "iron_golem", "snow_golem"),
            new CreatureCategory("bosses", "gui.criaturas.bosses", Material.WITHER_SPAWN_EGG,
                    "wither", "ender_dragon")
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
        openMain(player);
        return true;
    }

    public void open(Player player) {
        openMain(player);
    }

    private void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(MAIN_TITLE));

        setCategory(inventory, 10, CATEGORIES[0]);
        setCategory(inventory, 12, CATEGORIES[1]);
        setCategory(inventory, 14, CATEGORIES[2]);
        setCategory(inventory, 16, CATEGORIES[3]);

        player.openInventory(inventory);
    }

    private void openCategory(Player player, Category category) {
        switch (category.key) {
            case "redstone" -> openLimitPage(player, REDSTONE_TITLE, category.key,
                    List.of("redstone", "hoppers", "pistoes", "pistoes-ativacoes-por-segundo", "observers"));
            case "criaturas" -> openCreatureCategories(player);
            case "geradores" -> openLimitPage(player, GENERATORS_TITLE, category.key,
                    List.of("spawners"));
            case "decoracoes" -> openLimitPage(player, DECORATIONS_TITLE, category.key,
                    List.of("entidades", "itens", "xp-orbes"));
        }
    }

    private void openCreatureCategories(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(CREATURES_TITLE));

        int[] categorySlots = {9, 11, 13, 15, 17, 18, 20, 22, 24, 26};
        for (int i = 0; i < CREATURE_CATEGORIES.length; i++) {
            int slot = categorySlots[i];
            if (slot >= 27) {
                break;
            }
            CreatureCategory category = CREATURE_CATEGORIES[i];
            ItemStack item = new ItemStack(category.material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(plugin.getMessageManager().get(category.messageKey)));
                meta.setLore(List.of(
                        color("&7Clique para visualizar os limites."),
                        color("&7Cada criatura possui limite e raio próprios.")
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    private void openCreaturePage(Player player, CreatureCategory category, int page) {
        int perPage = 21;
        int start = page * perPage;
        int end = Math.min(start + perPage, category.mobs.size());

        Inventory inventory = Bukkit.createInventory(null, 27,
                color(plugin.getMessageManager().get(category.messageKey)));

        for (int i = start; i < end; i++) {
            String mob = category.mobs.get(i);
            Material icon = resolveSpawnEgg(mob);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&f" + formatMobName(mob)));
                meta.setLore(List.of(
                        color("&7Limite: &fNão configurado"),
                        color("&7Raio: &fNão configurado"),
                        "",
                        color("&8Configuração individual por criatura.")
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(i - start, item);
        }

        addNavigation(inventory, page > 0, end < category.mobs.size());
        player.openInventory(inventory);
    }

    private void openLimitPage(Player player, String title, String category, List<String> keys) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(title));

        int slot = 10;
        for (String key : keys) {
            Material material = materialForLimit(key);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(limitName(key)));
                meta.setLore(List.of(
                        color("&7Limite: &f" + currentLimit(player, key)),
                        color("&7Por chunk")
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot += 2;
        }

        setBackButton(inventory);
        player.openInventory(inventory);
    }

    private int currentLimit(Player player, String key) {
        return plugin.getConfigManager().getLimit(player.getWorld(), key, 0);
    }

    private void addNavigation(Inventory inventory, boolean previous, boolean next) {
        if (previous) {
            setButton(inventory, 21, Material.ARROW, "&fPágina anterior");
        }
        setBackButton(inventory);
        if (next) {
            setButton(inventory, 23, Material.ARROW, "&fPróxima página");
        }
    }

    private void setBackButton(Inventory inventory) {
        setButton(inventory, 22, Material.ARROW, "&fVoltar");
    }

    private void setButton(Inventory inventory, int slot, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private void setCategory(Inventory inventory, int slot, Category category) {
        ItemStack item = new ItemStack(category.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(plugin.getMessageManager().get(category.messageKey)));
            meta.setLore(List.of(color("&7Clique para visualizar os limites.")));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private Material materialForLimit(String key) {
        return switch (key) {
            case "hoppers" -> Material.HOPPER;
            case "pistoes" -> Material.PISTON;
            case "pistoes-ativacoes-por-segundo" -> Material.REDSTONE_TORCH;
            case "observers" -> Material.OBSERVER;
            case "spawners" -> Material.SPAWNER;
            case "entidades" -> Material.ARMOR_STAND;
            case "itens" -> Material.DIAMOND;
            case "xp-orbes" -> Material.EXPERIENCE_BOTTLE;
            default -> Material.REDSTONE;
        };
    }

    private String limitName(String key) {
        return switch (key) {
            case "hoppers" -> "&bFunis";
            case "pistoes" -> "&bPistões";
            case "pistoes-ativacoes-por-segundo" -> "&bAtivações de pistão por segundo";
            case "observers" -> "&bObservadores";
            case "spawners" -> "&dSpawners";
            case "entidades" -> "&eEntidades";
            case "itens" -> "&eItens";
            case "xp-orbes" -> "&eOrbes de XP";
            default -> "&bRedstone";
        };
    }

    private Material resolveSpawnEgg(String mob) {
        String material = mob.toUpperCase() + "_SPAWN_EGG";
        try {
            return Material.valueOf(material);
        } catch (IllegalArgumentException ignored) {
            return Material.PLAYER_HEAD;
        }
    }

    private String formatMobName(String mob) {
        String[] parts = mob.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private String color(String text) {
        return text.replace('&', '§');
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isLimitsInventory(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!isLimitsInventory(title)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (isTitle(title, MAIN_TITLE)) {
            if (slot == 10) openCategory(player, CATEGORIES[0]);
            else if (slot == 12) openCategory(player, CATEGORIES[1]);
            else if (slot == 14) openCategory(player, CATEGORIES[2]);
            else if (slot == 16) openCategory(player, CATEGORIES[3]);
            return;
        }

        if (isTitle(title, CREATURES_TITLE)) {
            for (CreatureCategory category : CREATURE_CATEGORIES) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() == category.material) {
                    openCreaturePage(player, category, 0);
                    return;
                }
            }
            return;
        }

        if (slot == 22) {
            openMain(player);
            return;
        }

        if (isTitle(title, REDSTONE_TITLE) || isTitle(title, GENERATORS_TITLE) || isTitle(title, DECORATIONS_TITLE)) {
            return;
        }

        for (CreatureCategory category : CREATURE_CATEGORIES) {
            if (isTitle(title, plugin.getMessageManager().get(category.messageKey))) {
                int page = 0;
                if (slot == 23) page = 1;
                if (slot == 21) page = 0;
                if (slot == 22) {
                    openCreatureCategories(player);
                    return;
                }
                openCreaturePage(player, category, page);
                return;
            }
        }
    }

    private boolean isLimitsInventory(String title) {
        if (title == null) return false;
        if (isTitle(title, MAIN_TITLE) || isTitle(title, REDSTONE_TITLE)
                || isTitle(title, CREATURES_TITLE) || isTitle(title, GENERATORS_TITLE)
                || isTitle(title, DECORATIONS_TITLE)) {
            return true;
        }
        for (CreatureCategory category : CREATURE_CATEGORIES) {
            if (isTitle(title, plugin.getMessageManager().get(category.messageKey))) return true;
        }
        return false;
    }

    private boolean isTitle(String actual, String expected) {
        return color(expected).equals(actual);
    }

    private record Category(String key, String messageKey, Material material) {}
    private record CreatureCategory(String key, String messageKey, Material material, List<String> mobs) {
        CreatureCategory(String key, String messageKey, Material material, String... mobs) {
            this(key, messageKey, material, List.of(mobs));
        }
    }
}
