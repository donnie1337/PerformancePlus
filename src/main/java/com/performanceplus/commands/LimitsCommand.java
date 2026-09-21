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
            ItemStack icon = category.key.equals("criaturas")
                    ? criarCabecaCombatePlus("zombie", "&a&lCriaturas", List.of("&7Clique para visualizar os limites."))
                    : createConfiguredItem(path, category.material, category.key, List.of("&7Clique para visualizar os limites."), -1);
            inventory.setItem(slot, icon);
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

    private record CreatureCategory(String key, String title, String headMob, List<String> mobs) {}

    private static final CreatureCategory[] CREATURE_CATEGORIES = {
            new CreatureCategory("animais-terrestres", "&aAnimais terrestres", "panda", List.of(
                    "bee", "allay", "donkey", "goat", "camel", "horse", "rabbit", "sniffer", "chicken",
                    "cat", "ocelot", "llama", "wolf", "mooshroom", "bat", "mule", "sheep", "panda",
                    "parrot", "pig", "fox", "frog", "armadillo", "polar_bear", "cow"
            )),
            new CreatureCategory("animais-aquaticos", "&bAnimais aquáticos", "axolotl", List.of(
                    "axolotl", "cod", "dolphin", "drowned", "elder_guardian", "glow_squid", "guardian",
                    "pufferfish", "salmon", "squid", "tadpole", "tropical_fish", "turtle", "nautilus"
            )),
            new CreatureCategory("monstros", "&cMonstros", "zombie", List.of(
                    "bogged", "breeze", "cave_spider", "creaking", "creeper", "drowned", "elder_guardian",
                    "endermite", "guardian", "husk", "phantom", "silverfish", "skeleton", "slime",
                    "spider", "stray", "warden", "witch", "zombie", "zombie_villager"
            )),
            new CreatureCategory("pillagers", "&6Saqueadores", "pillager", List.of(
                    "evoker", "illusioner", "pillager", "ravager", "vex", "vindicator"
            )),
            new CreatureCategory("aldeoes-traders", "&eAldeões e Comerciantes", "villager", List.of(
                    "villager", "wandering_trader"
            )),
            new CreatureCategory("cavalos-especiais", "&dCavalos especiais", "horse", List.of(
                    "camel", "donkey", "horse", "llama", "mule", "skeleton_horse", "trader_llama", "zombie_horse"
            )),
            new CreatureCategory("golems", "&fGolems", "iron_golem", List.of(
                    "iron_golem", "snow_golem"
            )),
            new CreatureCategory("end", "&5End", "enderman", List.of(
                    "ender_dragon", "enderman", "endermite", "shulker"
            )),
            new CreatureCategory("nether", "&4Nether", "piglin", List.of(
                    "blaze", "ghast", "hoglin", "magma_cube", "piglin", "piglin_brute",
                    "strider", "wither_skeleton", "zoglin", "zombified_piglin"
            )),
            new CreatureCategory("bosses", "&4Chefes", "wither", List.of(
                    "elder_guardian", "ender_dragon", "warden", "wither"
            ))
    };

    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14,
            19, 20, 21, 22, 23
    };

    private static final int[] HEAD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int FIRST_CREATURE_PAGE_SIZE = 54;
    private static final int OTHER_CREATURE_PAGE_SIZE = 36;
    private static final int CREATURE_BACK_SLOT = 48;
    private static final int CREATURE_PREVIOUS_SLOT = 31;
    private static final int CREATURE_NEXT_SLOT = 50;

    private void openCreatureCategories(Player player) {
        String path = "criaturas";
        int size = guiInt(path, "tamanho", 45);
        String title = guiString(path, "titulo", "&8&lLimites &8• &aCriaturas");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        for (int i = 0; i < CREATURE_CATEGORIES.length; i++) {
            CreatureCategory category = CREATURE_CATEGORIES[i];
            int slot = guiInt(path + ".categorias." + category.key, "slot",
                    CATEGORY_SLOTS[i]);

            String itemPath = path + ".categorias." + category.key;
            inventory.setItem(slot, criarCabecaCombatePlus(
                    category.headMob,
                    category.title,
                    List.of("&7Clique para visualizar os limites.")
            ));
        }

        setNavigation(inventory, "criaturas-menu", "voltar");
        player.openInventory(inventory);
    }

    private void openCreaturePage(Player player, CreatureCategory category, int page) {
        List<String> entries = category.mobs;
        int perPage = HEAD_SLOTS.length;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));

        String path = "criatura-itens";
        int size = page == 0
                ? guiInt(path, "tamanho-primeira-pagina", FIRST_CREATURE_PAGE_SIZE)
                : guiInt(path, "tamanho-outras-paginas", OTHER_CREATURE_PAGE_SIZE);
        String title = guiString(path, "titulo-" + category.key,
                "&8&lLimites &8• " + category.title);

        Inventory inventory = Bukkit.createInventory(null, size,
                color(title + " &8• &7" + (page + 1) + "/" + (maxPage + 1)));

        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            String mob = entries.get(i);
            int slot = HEAD_SLOTS[i - startIndex];
            ItemStack head = criarCabecaCombatePlus(
                    mob,
                    "&f&l" + formatMobName(mob),
                    List.of("&7Limite por chunk: &f" + currentLimit(player, mob))
            );
            inventory.setItem(slot, head);
        }

        if (page == 0) {
            setNavigation(inventory, "criaturas-pagina", "voltar", CREATURE_BACK_SLOT);
            if (page < maxPage) {
                setNavigation(inventory, "criaturas-pagina", "proxima-pagina", CREATURE_NEXT_SLOT);
            }
        } else {
            setNavigation(inventory, "criaturas-pagina", "pagina-anterior", CREATURE_PREVIOUS_SLOT);
        }

        player.openInventory(inventory);
    }

    private CreatureCategory findCreatureCategory(String key) {
        for (CreatureCategory category : CREATURE_CATEGORIES) {
            if (category.key.equals(key)) return category;
        }
        return null;
    }

    private CreatureCategory findCreatureCategoryByTitle(String title) {
        if (title == null) return null;
        for (CreatureCategory category : CREATURE_CATEGORIES) {
            String base = color(guiString("criatura-itens", "titulo-" + category.key,
                    "&8&lLimites &8• " + category.title));
            if (title.startsWith(base)) return category;
        }
        return null;
    }

    private ItemStack criarCabecaCombatePlus(String nome, String titulo, List<String> lore) {
        ItemStack item = null;
        try {
            org.bukkit.plugin.Plugin combate = Bukkit.getPluginManager().getPlugin("CombatePlus");
            if (combate != null) {
                Object manager = combate.getClass().getMethod("getCabecasManager").invoke(combate);
                if (manager != null) {
                    Object resultado = manager.getClass()
                            .getMethod("criarCabecaPorNome", String.class)
                            .invoke(manager, nome);
                    if (resultado instanceof ItemStack stack) item = stack.clone();
                }
            }
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Não foi possível acessar o sistema de cabeças do CombatePlus.");
        }

        if (item == null) item = new ItemStack(Material.PLAYER_HEAD);

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
        setNavigation(inventory, page, key, -1);
    }

    private void setNavigation(Inventory inventory, String page, String key, int forcedSlot) {
        String specificPath = "navegacao." + page + "." + key;
        String path = gui(specificPath) != null ? specificPath : "navegacao." + key;
        int slot = forcedSlot >= 0 ? forcedSlot
                : guiInt(path, "slot", key.equals("voltar") ? 36 : key.equals("pagina-anterior") ? 40 : 44);
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
        return switch (mob) {
            case "cow" -> "Vaca"; case "sheep" -> "Ovelha"; case "pig" -> "Porco"; case "chicken" -> "Galinha";
            case "rabbit" -> "Coelho"; case "fox" -> "Raposa"; case "wolf" -> "Lobo"; case "cat" -> "Gato";
            case "panda" -> "Panda"; case "mooshroom" -> "Coguvaca"; case "goat" -> "Cabra"; case "polar_bear" -> "Urso-polar";
            case "bee" -> "Abelha"; case "parrot" -> "Papagaio"; case "allay" -> "Allay"; case "sniffer" -> "Farejador";
            case "armadillo" -> "Tatu"; case "bat" -> "Morcego"; case "ocelot" -> "Jaguatirica";
            case "turtle" -> "Tartaruga"; case "dolphin" -> "Golfinho"; case "squid" -> "Lula"; case "glow_squid" -> "Lula brilhante";
            case "cod" -> "Bacalhau"; case "salmon" -> "Salmão"; case "tropical_fish" -> "Peixe tropical"; case "pufferfish" -> "Baiacu";
            case "axolotl" -> "Axolote"; case "nautilus" -> "Náutilo"; case "frog" -> "Sapo"; case "tadpole" -> "Girino";
            case "zombie" -> "Zumbi"; case "skeleton" -> "Esqueleto"; case "creeper" -> "Creeper"; case "spider" -> "Aranha";
            case "cave_spider" -> "Aranha das cavernas"; case "witch" -> "Bruxa"; case "slime" -> "Slime"; case "phantom" -> "Fantasma";
            case "silverfish" -> "Traça"; case "endermite" -> "Endermite"; case "guardian" -> "Guardião"; case "elder_guardian" -> "Guardião mestre";
            case "drowned" -> "Afogado"; case "husk" -> "Zumbi-múmia"; case "stray" -> "Esqueleto glacial"; case "bogged" -> "Esqueleto do pântano";
            case "warden" -> "Warden"; case "breeze" -> "Brisa"; case "creaking" -> "Creaking"; case "zombie_villager" -> "Aldeão zumbi";
            case "pillager" -> "Saqueador"; case "vindicator" -> "Vingador"; case "evoker" -> "Invocador"; case "illusioner" -> "Ilusionista";
            case "ravager" -> "Devastador"; case "vex" -> "Vex"; case "villager" -> "Aldeão"; case "wandering_trader" -> "Comerciante ambulante";
            case "horse" -> "Cavalo"; case "donkey" -> "Burro"; case "mule" -> "Mula"; case "skeleton_horse" -> "Cavalo esqueleto";
            case "zombie_horse" -> "Cavalo zumbi"; case "llama" -> "Lhama"; case "trader_llama" -> "Lhama do comerciante"; case "camel" -> "Camelo";
            case "iron_golem" -> "Golem de ferro"; case "snow_golem" -> "Golem de neve"; case "enderman" -> "Enderman"; case "shulker" -> "Shulker";
            case "ender_dragon" -> "Dragão do End"; case "blaze" -> "Blaze"; case "ghast" -> "Ghast"; case "magma_cube" -> "Cubo de magma";
            case "piglin" -> "Piglin"; case "piglin_brute" -> "Piglin bruto"; case "zombified_piglin" -> "Piglin zumbificado";
            case "hoglin" -> "Hoglin"; case "zoglin" -> "Zoglin"; case "wither_skeleton" -> "Esqueleto wither";
            case "strider" -> "Lavagante"; case "wither" -> "Wither"; default -> mob;
        };
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
        return findCreatureCategoryByTitle(title) != null;
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
            if (slot == guiInt("navegacao.criaturas-menu.voltar", "slot", 22)
                    || slot == guiInt("navegacao.voltar", "slot", 49)) {
                openMain(player);
                return;
            }

            for (int i = 0; i < CREATURE_CATEGORIES.length; i++) {
                CreatureCategory category = CREATURE_CATEGORIES[i];
                int categorySlot = guiInt(
                        "criaturas.categorias." + category.key,
                        "slot",
                        CATEGORY_SLOTS[i]
                );
                if (slot == categorySlot) {
                    openCreaturePage(player, category, 0);
                    return;
                }
            }
            return;
        }

        if (isCreaturePageTitle(title)) {
            CreatureCategory category = findCreatureCategoryByTitle(title);
            if (category == null) return;

            int page = extrairPagina(title);
            int maxPage = Math.max(0, (category.mobs.size() - 1) / HEAD_SLOTS.length);

            if (page == 0 && slot == CREATURE_BACK_SLOT) {
                openCreatureCategories(player);
                return;
            }
            if (page > 0 && slot == CREATURE_PREVIOUS_SLOT) {
                openCreaturePage(player, category, page - 1);
                return;
            }
            if (page < maxPage && slot == CREATURE_NEXT_SLOT) {
                openCreaturePage(player, category, page + 1);
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
        if (title == null) return false;
        String configuredTitle = guiString("paginas." + page, "titulo", "&8&lLimites");
        return color(configuredTitle).equals(title);
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