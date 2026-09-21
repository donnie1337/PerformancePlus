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

        ConfigurationSection configuredItems = gui(path + ".itens");
        if (configuredItems != null) {
            for (String key : configuredItems.getKeys(false)) {
                String itemPath = path + ".itens." + key;
                int slot = guiInt(itemPath, "slot", -1);
                if (slot < 0 || slot >= size) continue;

                String limitKey = guiString(itemPath, "chave-limite", key);
                int limit = currentLimit(player, limitKey);
                inventory.setItem(slot, createConfiguredItem(itemPath, materialForLimit(key), key,
                        List.of("&7Limite: &f{limite}", "&7Por chunk"), limit));
            }
        } else {
            for (String key : keys) {
                String itemPath = "itens." + key;
                int slot = guiInt(itemPath, "slot", -1);
                if (slot < 0 || slot >= size) continue;
                inventory.setItem(slot, createConfiguredItem(itemPath, materialForLimit(key), key,
                        List.of("&7Limite: &f{limite}", "&7Por chunk"), currentLimit(player, key)));
            }
        }

        setNavigation(inventory, page, "voltar");
        player.openInventory(inventory);
    }

    private static final List<String> HOSTILE_MOBS = List.of(
            "zombie", "skeleton", "creeper", "spider", "cave_spider", "witch", "slime",
            "phantom", "enderman", "silverfish", "endermite", "blaze", "ghast", "magma_cube",
            "piglin", "piglin_brute", "zombified_piglin", "hoglin", "zoglin", "wither_skeleton",
            "shulker", "guardian", "elder_guardian", "drowned", "husk", "stray", "bogged",
            "pillager", "vindicator", "evoker", "illusioner", "ravager", "vex", "warden",
            "wither", "ender_dragon", "breeze", "creaking", "zombie_villager"
    );

    private static final List<String> ANIMALS = List.of(
            "cow", "sheep", "pig", "chicken", "horse", "donkey", "mule", "rabbit", "fox",
            "wolf", "cat", "panda", "mooshroom", "goat", "polar_bear", "turtle", "dolphin",
            "squid", "glow_squid", "cod", "salmon", "tropical_fish", "pufferfish", "axolotl",
            "frog", "tadpole", "bat", "bee", "parrot", "allay", "sniffer", "armadillo",
            "camel", "llama", "ocelot", "strider"
    );

    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private void openCreatureCategories(Player player) {
        String path = "criaturas";
        int size = guiInt(path, "tamanho", 27);
        String title = guiString(path, "titulo", "&8&lLimites &8• &aCriaturas");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        ItemStack criaturas = criarCabecaCombatePlus("skeleton", "&f&lCriaturas",
                List.of("&7Clique para visualizar as criaturas hostis."));
        ItemStack animais = criarCabecaCombatePlus("panda", "&a&lAnimais",
                List.of("&7Clique para visualizar os animais."));

        inventory.setItem(guiInt(path, "criaturas-slot", 11), criaturas);
        inventory.setItem(guiInt(path, "animais-slot", 15), animais);

        setNavigation(inventory, "criaturas-menu", "voltar");
        player.openInventory(inventory);
    }

    private void openCreaturePage(Player player, boolean animais, int page) {
        List<String> entries = animais ? ANIMALS : HOSTILE_MOBS;
        String type = animais ? "animais" : "hostis";
        int perPage = HEAD_SLOTS.length;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));

        String path = "criatura-itens";
        int size = guiInt(path, "tamanho", 45);
        String title = guiString(path, animais ? "titulo-animais" : "titulo-criaturas",
                animais ? "&8&lLimites &8• &aAnimais" : "&8&lLimites &8• &cMobs hostis");

        Inventory inventory = Bukkit.createInventory(null, size,
                color(title + " &8• &7" + (page + 1) + "/" + (maxPage + 1)));

        int start = page * perPage;
        int end = Math.min(start + perPage, entries.size());

        for (int i = start; i < end; i++) {
            String mob = entries.get(i);
            int slot = HEAD_SLOTS[i - start];
            ItemStack head = criarCabecaCombatePlus(mob, "&f&l" + formatMobName(mob),
                    List.of(
                            "&7Limite por chunk: &f" + currentLimit(player, mob)
                    ));
            inventory.setItem(slot, head);
        }

        setNavigation(inventory, "criaturas-pagina", "voltar");
        if (page > 0) setNavigation(inventory, "criaturas-pagina", "pagina-anterior");
        if (page < maxPage) setNavigation(inventory, "criaturas-pagina", "proxima-pagina");

        player.openInventory(inventory);
    }

    private ItemStack criarCabecaCombatePlus(String nome, String titulo, List<String> lore) {
        ItemStack item = null;
        try {
            org.bukkit.plugin.Plugin combate = Bukkit.getPluginManager().getPlugin("CombatePlus");
            if (combate != null) {
                Object manager = combate.getClass().getMethod("getCabecasManager").invoke(combate);
                if (manager != null) {
                    Object resultado = manager.getClass().getMethod("criarCabecaPorNome", String.class).invoke(manager, nome);
                    if (resultado instanceof ItemStack stack) {
                        item = stack.clone();
                    }
                }
            }
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Não foi possível acessar o sistema de cabeças do CombatePlus.");
        }

        if (item == null) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(titulo));
            meta.setLore(lore.stream().map(this::color).toList());
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
        int slot = guiInt(path, "slot", key.equals("voltar") ? (inventory.getSize() - 5) : key.equals("pagina-anterior") ? 38 : 42);
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

    private boolean isCreaturePageTitle(String title) {
        if (title == null) return false;
        return title.startsWith(color("&8&lLimites &8• &cMobs hostis"))
                || title.startsWith(color("&8&lLimites &8• &aAnimais"));
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
            if (slot == guiInt("criaturas", "criaturas-slot", 11)) {
                openCreaturePage(player, false, 0);
                return;
            }
            if (slot == guiInt("criaturas", "animais-slot", 15)) {
                openCreaturePage(player, true, 0);
                return;
            }
            if (slot == guiInt("navegacao.criaturas-menu.voltar", "slot", 22)
                    || slot == guiInt("navegacao.voltar", "slot", 49)) {
                openMain(player);
            }
            return;
        }

        if (isCreaturePageTitle(title)) {
            boolean animais = title.startsWith(color("&8&lLimites &8• &aAnimais"));
            int page = extrairPagina(title);
            List<String> entries = animais ? ANIMALS : HOSTILE_MOBS;
            int maxPage = Math.max(0, (entries.size() - 1) / HEAD_SLOTS.length);

            if (slot == guiInt("navegacao.criaturas-pagina.voltar", "slot", 40)
                    || slot == guiInt("navegacao.voltar", "slot", 49)) {
                openCreatureCategories(player);
                return;
            }
            if (slot == guiInt("navegacao.criaturas-pagina.pagina-anterior", "slot", 38)) {
                openCreaturePage(player, animais, Math.max(0, page - 1));
                return;
            }
            if (slot == guiInt("navegacao.criaturas-pagina.proxima-pagina", "slot", 42)) {
                if (page < maxPage) openCreaturePage(player, animais, page + 1);
            }
            return;
        }

        if (isLimitPageTitle(title, "redstone") || isLimitPageTitle(title, "geradores") || isLimitPageTitle(title, "decoracoes")) {
            if (slot == guiInt("navegacao.redstone.voltar", "slot", guiInt("navegacao.voltar", "slot", 49))
                    || slot == guiInt("navegacao.geradores.voltar", "slot", guiInt("navegacao.voltar", "slot", 49))
                    || slot == guiInt("navegacao.decoracoes.voltar", "slot", guiInt("navegacao.voltar", "slot", 49))) {
                openMain(player);
            }
        }
    }

    private int extrairPagina(String title) {
        int separator = title.lastIndexOf(" • ");
        if (separator < 0) return 0;
        String pagePart = title.substring(separator + 3);
        int slash = pagePart.indexOf('/');
        if (slash < 0) return 0;
        try {
            return Math.max(0, Integer.parseInt(pagePart.substring(0, slash)) - 1);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isLimitPageTitle(String title, String page) {
        return color(guiString("paginas." + page, "titulo", "&8&lLimites")).equals(title);
    }

    private boolean isLimitsInventory(String title) {
        if (title == null) return false;
        return isMainTitle(title) || isCategoryTitle(title)
                || isLimitPageTitle(title, "redstone")
                || isLimitPageTitle(title, "geradores")
                || isLimitPageTitle(title, "decoracoes")
                || isCreaturePageTitle(title);
    }

    private record Category(String key, Material material) {}

}