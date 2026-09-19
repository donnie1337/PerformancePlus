package com.performanceplus.commands;

import com.performanceplus.PerformancePlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

    private final PerformancePlus plugin;

    private static final Category[] CATEGORIES = {
            new Category("redstone", Material.REDSTONE),
            new Category("criaturas", Material.CREEPER_SPAWN_EGG),
            new Category("geradores", Material.SPAWNER),
            new Category("decoracoes", Material.ARMOR_STAND)
    };

    private static final CreatureCategory[] CREATURE_CATEGORIES = {
            new CreatureCategory("animais-terrestres", "cow", "sheep", "pig", "chicken", "horse", "donkey", "mule", "rabbit", "fox", "wolf"),
            new CreatureCategory("animais-aquaticos", "cod", "salmon", "tropical_fish", "pufferfish", "squid", "glow_squid", "dolphin", "turtle", "axolotl", "guardian"),
            new CreatureCategory("monstros-comuns", "zombie", "skeleton", "creeper", "spider", "cave_spider", "witch", "slime", "phantom", "enderman", "silverfish"),
            new CreatureCategory("nether", "blaze", "ghast", "magma_cube", "piglin", "piglin_brute", "zombified_piglin", "hoglin", "zoglin", "wither_skeleton", "strider"),
            new CreatureCategory("end", "enderman", "shulker", "ender_dragon"),
            new CreatureCategory("pillagers", "pillager", "vindicator", "evoker", "ravager", "illusioner"),
            new CreatureCategory("aldeoes-traders", "villager", "wandering_trader"),
            new CreatureCategory("cavalos-especiais", "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel"),
            new CreatureCategory("golems", "iron_golem", "snow_golem"),
            new CreatureCategory("bosses", "wither", "ender_dragon")
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

    private ConfigurationSection gui(String path) {
        return plugin.getConfig().getConfigurationSection("gui." + path);
    }

    private String guiString(String path, String key, String def) {
        ConfigurationSection section = gui(path);
        return section == null ? def : section.getString(key, def);
    }

    private int guiInt(String path, String key, int def) {
        ConfigurationSection section = gui(path);
        return section == null ? def : section.getInt(key, def);
    }

    private List<String> guiLore(String path, String key, List<String> def) {
        ConfigurationSection section = gui(path);
        return section == null ? def : section.getStringList(key);
    }

    private Material guiMaterial(String path, String key, Material def) {
        String value = guiString(path, key, def.name());
        try {
            return Material.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return def;
        }
    }

    private String mainTitle() {
        return guiString("principal", "titulo", "&8&lLimites");
    }

    private void openMain(Player player) {
        int size = guiInt("principal", "tamanho", 27);
        Inventory inventory = Bukkit.createInventory(null, size, color(mainTitle()));

        for (Category category : CATEGORIES) {
            String path = "principal.categorias." + category.key;
            int slot = guiInt(path, "slot", -1);
            if (slot < 0 || slot >= size) continue;
            inventory.setItem(slot, createConfiguredItem(path, category.material, category.key, List.of("&7Clique para visualizar os limites."), -1));
        }

        player.openInventory(inventory);
    }

    private void openCategory(Player player, Category category) {
        switch (category.key) {
            case "redstone" -> openLimitPage(player, "redstone", REDSTONE_KEYS);
            case "criaturas" -> openCreatureCategories(player);
            case "geradores" -> openLimitPage(player, "geradores", List.of("spawners"));
            case "decoracoes" -> openLimitPage(player, "decoracoes", List.of("entidades", "itens", "xp-orbes"));
        }
    }

    private static final List<String> REDSTONE_KEYS = List.of(
            "suporte-armaduras", "carrinho-com-bau", "comparadores", "ejetores", "liberadores",
            "carrinho-com-fornalha", "carrinho-com-funil", "hoppers", "carrinho-de-mina",
            "observers", "pistoes", "blocos-de-redstone", "lampadas-de-redstone",
            "tochas-de-redstone", "po-de-redstone", "repetidores", "sensores-de-sculk",
            "pistoes-com-slime", "carrinho-com-dinamite"
    );

    private void openLimitPage(Player player, String page, List<String> keys) {
        String path = "paginas." + page;
        int size = guiInt(path, "tamanho", page.equals("redstone") ? 54 : 27);
        String title = guiString(path, "titulo", "&8&lLimites");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        for (String key : keys) {
            String itemPath = "itens." + key;
            int slot = guiInt(itemPath, "slot", -1);
            if (slot < 0 || slot >= size) continue;
            inventory.setItem(slot, createConfiguredItem(itemPath, materialForLimit(key), key,
                    List.of("&7Limite: &f{limite}", "&7Por chunk"), currentLimit(player, key)));
        }

        setNavigation(inventory, page, "voltar");
        player.openInventory(inventory);
    }

    private void openCreatureCategories(Player player) {
        String path = "criaturas";
        int size = guiInt(path, "tamanho", 27);
        String title = guiString(path, "titulo", "&8&lLimites &8• &aCriaturas");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        for (CreatureCategory category : CREATURE_CATEGORIES) {
            String categoryPath = path + ".categorias." + category.key;
            int slot = guiInt(categoryPath, "slot", -1);
            if (slot < 0 || slot >= size) continue;
            inventory.setItem(slot, createConfiguredItem(categoryPath, defaultCreatureCategoryMaterial(category), category.key,
                    List.of("&7Clique para visualizar os limites.", "&7Cada criatura possui limite e raio próprios."), -1));
        }

        player.openInventory(inventory);
    }

    private void openCreaturePage(Player player, CreatureCategory category, int page) {
        String path = "criatura-itens";
        int size = guiInt(path, "tamanho", 27);
        String title = guiString("criatura-paginas." + category.key, "titulo",
                guiString("criaturas.categorias." + category.key, "titulo", "&8&lLimites"));
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        int perPage = Math.max(1, guiInt(path, "itens-por-pagina", 21));
        int start = page * perPage;
        int end = Math.min(start + perPage, category.mobs.size());

        for (int i = start; i < end; i++) {
            String mob = category.mobs.get(i);
            String itemPath = path + "." + mob;
            int defaultSlot = i - start;
            int slot = guiInt(itemPath, "slot", defaultSlot);
            if (slot < 0 || slot >= size) continue;

            Material material = resolveSpawnEgg(mob);
            String defaultTitle = "&f" + formatMobName(mob);
            List<String> defaultLore = List.of("&7Limite: &fNão configurado", "&7Raio: &fNão configurado", "", "&8Configuração individual por criatura.");
            inventory.setItem(slot, createCreatureItem(itemPath, material, defaultLore, defaultTitle));
        }

        if (page > 0) setNavigation(inventory, "criaturas", "pagina-anterior");
        setNavigation(inventory, "criaturas", "voltar");
        if (end < category.mobs.size()) setNavigation(inventory, "criaturas", "proxima-pagina");

        player.openInventory(inventory);
    }

    private ItemStack createCreatureItem(String itemPath, Material material, List<String> fallbackLore, String fallbackTitle) {
        String defaultPath = "criatura-itens.padrao";
        Material configuredMaterial = guiMaterial(itemPath, "material", guiMaterial(defaultPath, "material", material));
        String title = guiString(itemPath, "titulo", guiString(defaultPath, "titulo", fallbackTitle));
        List<String> lore = guiLore(itemPath, "lore", guiLore(defaultPath, "lore", fallbackLore));
        List<String> parsedLore = new ArrayList<>();
        for (String line : lore) {
            parsedLore.add(color(line));
        }
        ItemStack item = new ItemStack(configuredMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(title));
            meta.setLore(parsedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfiguredItem(String path, Material fallbackMaterial, String key, List<String> fallbackLore, int limit) {
        return createConfiguredItem(path, fallbackMaterial, key, fallbackLore, limit, null);
    }

    private ItemStack createConfiguredItem(String path, Material fallbackMaterial, String key, List<String> fallbackLore, int limit, String fallbackTitle) {
        Material material = guiMaterial(path, "material", fallbackMaterial);
        String title = guiString(path, "titulo", fallbackTitle != null ? fallbackTitle : key);
        List<String> lore = guiLore(path, "lore", fallbackLore);

        List<String> parsedLore = new ArrayList<>();
        for (String line : lore) {
            parsedLore.add(color(line.replace("{limite}", String.valueOf(limit))));
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(title));
            meta.setLore(parsedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setNavigation(Inventory inventory, String page, String key) {
        String specificPath = "navegacao." + page + "." + key;
        String path = gui(specificPath) != null ? specificPath : "navegacao." + key;
        int slot = guiInt(path, "slot", key.equals("voltar") ? (inventory.getSize() - 5) : key.equals("pagina-anterior") ? 21 : 23);
        if (slot < 0 || slot >= inventory.getSize()) return;
        Material material = guiMaterial(path, "material", Material.ARROW);
        String title = guiString(path, "titulo", "&fVoltar");
        List<String> lore = guiLore(path, "lore", List.of());
        inventory.setItem(slot, createConfiguredItem(path, material, key, lore, -1, title));
    }

    private int currentLimit(Player player, String key) {
        return plugin.getConfigManager().getLimit(player.getWorld(), key, 0);
    }

    private Material materialForLimit(String key) {
        return switch (key) {
            case "suporte-armaduras" -> Material.ARMOR_STAND;
            case "carrinho-com-bau" -> Material.CHEST_MINECART;
            case "comparadores" -> Material.COMPARATOR;
            case "ejetores" -> Material.DISPENSER;
            case "liberadores" -> Material.DROPPER;
            case "carrinho-com-fornalha" -> Material.FURNACE_MINECART;
            case "carrinho-com-funil" -> Material.HOPPER_MINECART;
            case "hoppers" -> Material.HOPPER;
            case "carrinho-de-mina" -> Material.MINECART;
            case "observers" -> Material.OBSERVER;
            case "pistoes" -> Material.PISTON;
            case "blocos-de-redstone" -> Material.REDSTONE_BLOCK;
            case "lampadas-de-redstone" -> Material.REDSTONE_LAMP;
            case "tochas-de-redstone" -> Material.REDSTONE_TORCH;
            case "po-de-redstone" -> Material.REDSTONE;
            case "repetidores" -> Material.REPEATER;
            case "sensores-de-sculk" -> Material.SCULK_SENSOR;
            case "pistoes-com-slime" -> Material.STICKY_PISTON;
            case "carrinho-com-dinamite" -> Material.TNT_MINECART;
            case "spawners" -> Material.SPAWNER;
            case "entidades" -> Material.ARMOR_STAND;
            case "itens" -> Material.DIAMOND;
            case "xp-orbes" -> Material.EXPERIENCE_BOTTLE;
            default -> Material.REDSTONE;
        };
    }

    private Material defaultCreatureCategoryMaterial(CreatureCategory category) {
        return switch (category.key) {
            case "animais-terrestres" -> Material.COW_SPAWN_EGG;
            case "animais-aquaticos" -> Material.COD_SPAWN_EGG;
            case "monstros-comuns" -> Material.ZOMBIE_SPAWN_EGG;
            case "nether" -> Material.BLAZE_SPAWN_EGG;
            case "end" -> Material.ENDERMAN_SPAWN_EGG;
            case "pillagers" -> Material.PILLAGER_SPAWN_EGG;
            case "aldeoes-traders" -> Material.VILLAGER_SPAWN_EGG;
            case "cavalos-especiais" -> Material.HORSE_SPAWN_EGG;
            case "golems" -> Material.IRON_GOLEM_SPAWN_EGG;
            case "bosses" -> Material.WITHER_SPAWN_EGG;
            default -> Material.CHEST;
        };
    }

    private Material resolveSpawnEgg(String mob) {
        try {
            return Material.valueOf(mob.toUpperCase() + "_SPAWN_EGG");
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

    private boolean isMainTitle(String title) {
        return color(mainTitle()).equals(title);
    }

    private boolean isCategoryTitle(String title) {
        return color(guiString("criaturas", "titulo", "&8&lLimites &8• &aCriaturas")).equals(title);
    }

    private boolean isLimitPageTitle(String title, String page) {
        return color(guiString("paginas." + page, "titulo", "&8&lLimites")).equals(title);
    }

    private CreatureCategory findCreatureCategory(String title) {
        for (CreatureCategory category : CREATURE_CATEGORIES) {
            String configured = guiString("criatura-paginas." + category.key, "titulo", "");
            if (!configured.isEmpty() && color(configured).equals(title)) return category;
            String fallback = guiString("criaturas.categorias." + category.key, "titulo", "");
            if (!fallback.isEmpty() && color(fallback).equals(title)) return category;
        }
        return null;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isLimitsInventory(event.getView().getTitle())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!isLimitsInventory(title)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        if (isMainTitle(title)) {
            for (Category category : CATEGORIES) {
                String path = "principal.categorias." + category.key;
                if (slot == guiInt(path, "slot", -1)) {
                    openCategory(player, category);
                    return;
                }
            }
            return;
        }

        if (isCategoryTitle(title)) {
            for (CreatureCategory category : CREATURE_CATEGORIES) {
                String path = "criaturas.categorias." + category.key;
                if (slot == guiInt(path, "slot", -1)) {
                    openCreaturePage(player, category, 0);
                    return;
                }
            }
            return;
        }

        if (isLimitPageTitle(title, "redstone") || isLimitPageTitle(title, "geradores") || isLimitPageTitle(title, "decoracoes")) {
            if (slot == guiInt("navegacao.redstone.voltar", "slot", guiInt("navegacao.voltar", "slot", 49)) || slot == guiInt("navegacao.geradores.voltar", "slot", guiInt("navegacao.voltar", "slot", 49)) || slot == guiInt("navegacao.decoracoes.voltar", "slot", guiInt("navegacao.voltar", "slot", 49))) openMain(player);
            return;
        }

        CreatureCategory creatureCategory = findCreatureCategory(title);
        if (creatureCategory != null) {
            if (slot == guiInt("navegacao.criaturas.voltar", "slot", 22) || slot == guiInt("navegacao.voltar", "slot", 49)) {
                openCreatureCategories(player);
                return;
            }
            if (slot == guiInt("navegacao.criaturas.pagina-anterior", "slot", 21)) {
                openCreaturePage(player, creatureCategory, 0);
                return;
            }
            if (slot == guiInt("navegacao.criaturas.proxima-pagina", "slot", 23)) {
                openCreaturePage(player, creatureCategory, 1);
            }
        }
    }

    private boolean isLimitsInventory(String title) {
        if (title == null) return false;
        if (isMainTitle(title) || isCategoryTitle(title)
                || isLimitPageTitle(title, "redstone")
                || isLimitPageTitle(title, "geradores")
                || isLimitPageTitle(title, "decoracoes")) return true;
        return findCreatureCategory(title) != null;
    }

    private record Category(String key, Material material) {}

    private record CreatureCategory(String key, List<String> mobs) {
        CreatureCategory(String key, String... mobs) {
            this(key, List.of(mobs));
        }
    }
}
