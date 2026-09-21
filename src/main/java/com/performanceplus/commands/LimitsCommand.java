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
        if (args.length == 1 && args[0].equalsIgnoreCase("chunk")) {
            new PerformancePlusCommand(plugin).sendChunkInfo(sender);
            return true;
        }

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
        return guiString("principal", "titulo", "&8Limites");
    }

    private void openMain(Player player) {
        int size = guiInt("principal", "tamanho", 27);
        Inventory inventory = Bukkit.createInventory(null, size, color(mainTitle()));

        for (Category category : CATEGORIES) {
            String path = "principal.categorias." + category.key;
            int slot = guiInt(path, "slot", -1);
            if (slot < 0 || slot >= size) continue;
            ItemStack icon = category.key.equals("criaturas")
                    ? criarCabecaCombatePlus("zombie", guiString(path, "titulo", "&aCriaturas"),
                            guiLore(path, "lore", List.of("", "&7Animais e monstros do servidor.", "&8Limites por proximidade e raio.")))
                    : createConfiguredItem(path, category.material, category.key,
                            category.key.equals("decoracoes")
                                    ? List.of("", "&7Limite de entidades por chunk: &f{limite}", "&8Itens no chão e orbes de XP não possuem limite.")
                                    : List.of("&7Clique para visualizar os limites."),
                            category.key.equals("decoracoes") ? currentLimit(player, "entidades") : -1);
            inventory.setItem(slot, icon);
        }

        player.openInventory(inventory);
    }

    private void openCategory(Player player, Category category) {
        switch (category.key) {
            case "redstone" -> openLimitPage(player, "redstone", REDSTONE_KEYS);
            case "criaturas" -> openCreatureCategories(player);
            case "geradores" -> openLimitPage(player, "geradores", GENERATOR_KEYS);
            case "decoracoes" -> {
                // O limite único de decorações é exibido no menu principal.
            }
        }
    }

    // Somente limites reais de colocação por chunk. Os controles técnicos
    // de atualizações por tick permanecem ativos no arquivo de configuração.
    private static final List<String> REDSTONE_KEYS = List.of(
            "po-de-redstone", "blocos-de-redstone", "tochas-de-redstone", "lampadas-de-redstone",
            "comparadores", "repetidores", "ejetores", "liberadores", "hoppers",
            "carrinho-com-funil", "carrinho-de-mina", "carrinho-com-bau",
            "carrinho-com-fornalha", "carrinho-com-dinamite", "observers", "pistoes",
            "pistoes-com-slime", "sensores-de-sculk", "catalisadores-de-sculk",
            "emissores-de-sculk", "sinalizadores", "suporte-armaduras",
            "redstone", "pistoes-ativacoes-por-segundo"
    );

    private static final List<String> GENERATOR_KEYS = List.of(
            "spawners", "chunks-gerados-por-segundo"
    );

    private void openLimitPage(Player player, String page, List<String> keys) {
        openLimitPage(player, page, keys, 0);
    }

    private void openLimitPage(Player player, String pageKey, List<String> keys, int page) {
        int perPage = HEAD_SLOTS.length;
        int maxPage = Math.max(0, (keys.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));

        String path = "paginas." + pageKey;
        boolean hasNextPage = maxPage > 0;
        int size = page == 0
                ? guiInt(path, hasNextPage ? "tamanho-primeira-pagina" : "tamanho-pagina-unica",
                        hasNextPage ? FIRST_CREATURE_PAGE_SIZE : SINGLE_CREATURE_PAGE_SIZE)
                : guiInt(path, "tamanho-outras-paginas", OTHER_CREATURE_PAGE_SIZE);
        String title = guiString(path, "titulo", "&8Limites");
        Inventory inventory = Bukkit.createInventory(null, size,
                color(title + " &8• &7Página " + (page + 1) + "&8/&7" + (maxPage + 1)));

        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, keys.size());
        for (int index = startIndex; index < endIndex; index++) {
            String key = keys.get(index);
            inventory.setItem(HEAD_SLOTS[index - startIndex],
                    createLimitItem(key, currentLimit(player, key)));
        }

        int leftArrowSlot;
        if (page == 0) {
            leftArrowSlot = hasNextPage ? CREATURE_BACK_SLOT
                    : size <= OTHER_CREATURE_PAGE_SIZE ? CREATURE_PREVIOUS_SLOT : SINGLE_CREATURE_BACK_SLOT;
            setNavigation(inventory, pageKey, "voltar", leftArrowSlot);
            if (page < maxPage) {
                setNavigation(inventory, pageKey, "proxima-pagina", CREATURE_NEXT_SLOT);
            }
        } else {
            leftArrowSlot = CREATURE_PREVIOUS_SLOT;
            setNavigation(inventory, pageKey, "pagina-anterior", leftArrowSlot);
        }
        setChunkInformationBook(inventory, leftArrowSlot - 2);

        player.openInventory(inventory);
    }

    private record CreatureCategory(String key, String title, String headMob, List<String> mobs) {}

    private static final CreatureCategory[] CREATURE_CATEGORIES = {
            new CreatureCategory("animais-terrestres", "&aAnimais terrestres", "panda", List.of(
                    "bee", "allay", "donkey", "goat", "camel", "horse", "rabbit", "sniffer", "chicken",
                    "cat", "ocelot", "llama", "trader_llama", "wolf", "mooshroom", "bat", "mule",
                    "sheep", "panda", "parrot", "pig", "fox", "frog", "armadillo", "polar_bear", "cow"
            )),
            new CreatureCategory("animais-aquaticos", "&bAnimais aquáticos", "axolotl", List.of(
                    "axolotl", "cod", "dolphin", "drowned", "elder_guardian", "glow_squid", "guardian",
                    "pufferfish", "salmon", "squid", "tadpole", "tropical_fish", "turtle", "nautilus"
            )),
            new CreatureCategory("monstros", "&cMonstros", "zombie", List.of(
                    "zombie_villager", "spider", "cave_spider", "breeze", "witch", "creaking", "creeper",
                    "skeleton", "bogged", "stray", "phantom", "slime", "silverfish", "zombie", "husk"
            )),
            new CreatureCategory("pillagers", "&6Saqueadores", "pillager", List.of(
                    "evoker", "illusioner", "pillager", "ravager", "vex", "vindicator"
            )),
            new CreatureCategory("aldeoes-traders", "&eAldeões e Comerciantes", "villager", List.of(
                    "villager", "wandering_trader"
            )),
            new CreatureCategory("monstros-especiais", "&dMonstros especiais", "charged_creeper", List.of(
                    "charged_creeper"
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
    private static final int SINGLE_CREATURE_PAGE_SIZE = 45;
    private static final int OTHER_CREATURE_PAGE_SIZE = 36;
    private static final int CREATURE_BACK_SLOT = 48;
    private static final int SINGLE_CREATURE_BACK_SLOT = 40;
    private static final int CREATURE_PREVIOUS_SLOT = 31;
    private static final int CREATURE_NEXT_SLOT = 50;

    private void openCreatureCategories(Player player) {
        String path = "criaturas";
        int size = guiInt(path, "tamanho", 45);
        String title = guiString(path, "titulo", "&8Limites &8• &aCriaturas");
        Inventory inventory = Bukkit.createInventory(null, size, color(title));

        for (int i = 0; i < CREATURE_CATEGORIES.length; i++) {
            CreatureCategory category = CREATURE_CATEGORIES[i];
            int slot = guiInt(path + ".categorias." + category.key, "slot",
                    CATEGORY_SLOTS[i]);

            String itemPath = path + ".categorias." + category.key;
            inventory.setItem(slot, criarCabecaCombatePlus(
                    category.headMob,
                    guiString(itemPath, "titulo", category.title),
                    guiLore(itemPath, "lore", List.of(
                            "",
                            "&7Clique para visualizar os limites.",
                            "&8Limites por proximidade."
                    ))
            ));
        }

        int backSlot = guiInt("navegacao.criaturas-menu.voltar", "slot", SINGLE_CREATURE_BACK_SLOT);
        setNavigation(inventory, "criaturas-menu", "voltar", backSlot);
        setChunkInformationBook(inventory, backSlot - 2);
        player.openInventory(inventory);
    }

    private void openCreaturePage(Player player, CreatureCategory category, int page) {
        List<String> entries = category.mobs;
        int perPage = HEAD_SLOTS.length;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));

        String path = "criatura-itens";
        boolean hasNextPage = maxPage > 0;
        int size = page == 0
                ? guiInt(path, hasNextPage ? "tamanho-primeira-pagina" : "tamanho-pagina-unica",
                        hasNextPage ? FIRST_CREATURE_PAGE_SIZE : SINGLE_CREATURE_PAGE_SIZE)
                : guiInt(path, "tamanho-outras-paginas", OTHER_CREATURE_PAGE_SIZE);
        String title = "&7Limites &8• &7" + stripColors(category.title);

        Inventory inventory = Bukkit.createInventory(null, size,
                color(title + " &8• &7Página " + (page + 1) + "&8/&7" + (maxPage + 1)));

        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            String mob = entries.get(i);
            int slot = HEAD_SLOTS[i - startIndex];
            ItemStack head = criarCabecaCombatePlus(
                    mob,
                    "&f" + formatMobName(mob),
                    List.of(
                            "",
                            "&7Limite por proximidade: &f" + currentCreatureLimit(player, mob),
                            "&7(Raio de alcance: &f" + currentRadius(player, mob) + " blocos&7)"
                    )
            );
            inventory.setItem(slot, head);
        }

        int leftArrowSlot;
        if (page == 0) {
            leftArrowSlot = hasNextPage ? CREATURE_BACK_SLOT : SINGLE_CREATURE_BACK_SLOT;
            setNavigation(inventory, "criaturas-pagina", "voltar", leftArrowSlot);
            if (page < maxPage) {
                setNavigation(inventory, "criaturas-pagina", "proxima-pagina", CREATURE_NEXT_SLOT);
            }
        } else {
            leftArrowSlot = CREATURE_PREVIOUS_SLOT;
            setNavigation(inventory, "criaturas-pagina", "pagina-anterior", leftArrowSlot);
        }
        setChunkInformationBook(inventory, leftArrowSlot - 2);

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
            String base = color("&7Limites &8• &7" + stripColors(category.title));
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

    private void setChunkInformationBook(Inventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) return;

        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(color("&eInformações sobre Chunk"));
        meta.setLore(List.of(
                color("&7"),
                color("&f• &7Para depurar a chunk atual, use:"),
                color("&e  /limite chunk"),
                color("&7"),
                color("&f• &7Uma chunk mede &f16 x 16 blocos&7."),
                color("&7  São &f256 blocos por camada."),
                color("&7"),
                color("&f• &7Criaturas usam limite por proximidade."),
                color("&7  Funis, redstone e mecanismos usam chunk.")
        ));
        book.setItemMeta(meta);
        inventory.setItem(slot, book);
    }

    private ItemStack createLimitItem(String key, int limit) {
        ItemStack item = new ItemStack(materialForLimit(key));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color("&f" + formatLimitName(key)));
        meta.setLore(limitLore(key, limit).stream().map(this::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private List<String> limitLore(String key, int limit) {
        return List.of(
                "",
                "&7" + limitScopeLabel(key) + ": &f" + limit,
                "&7(" + limitScopeDetail(key) + "&7)"
        );
    }

    private String limitScopeLabel(String key) {
        return switch (key) {
            case "redstone" -> "Atualizações por chunk/tick";
            case "pistoes-ativacoes-por-segundo", "chunks-gerados-por-segundo" -> "Limite por segundo";
            default -> "Limite por chunk";
        };
    }

    private String limitScopeDetail(String key) {
        return switch (key) {
            case "redstone" -> "Monitorado a cada &ftick";
            case "pistoes-ativacoes-por-segundo", "chunks-gerados-por-segundo" -> "Monitorado por &fsegundo";
            default -> "Área: &f16 x 16 blocos";
        };
    }

    private String formatLimitName(String key) {
        return switch (key) {
            case "redstone" -> "Atualizações de redstone";
            case "suporte-armaduras" -> "Suporte de armaduras";
            case "carrinho-com-bau" -> "Carrinho com baú";
            case "comparadores" -> "Comparadores";
            case "ejetores" -> "Ejetores";
            case "liberadores" -> "Liberadores";
            case "carrinho-com-fornalha" -> "Carrinho com fornalha";
            case "carrinho-com-funil" -> "Carrinho com funil";
            case "hoppers" -> "Funis";
            case "carrinho-de-mina" -> "Carrinho de mina";
            case "observers" -> "Observers";
            case "pistoes" -> "Pistões";
            case "pistoes-ativacoes-por-segundo" -> "Ativações de pistão";
            case "blocos-de-redstone" -> "Blocos de redstone";
            case "lampadas-de-redstone" -> "Lâmpadas de redstone";
            case "tochas-de-redstone" -> "Tochas de redstone";
            case "po-de-redstone" -> "Pó de redstone";
            case "repetidores" -> "Repetidores";
            case "sensores-de-sculk" -> "Sensores de sculk";
            case "catalisadores-de-sculk" -> "Catalisadores de sculk";
            case "emissores-de-sculk" -> "Emissores de sculk";
            case "sinalizadores" -> "Sinalizadores";
            case "pistoes-com-slime" -> "Pistões com slime";
            case "carrinho-com-dinamite" -> "Carrinho com dinamite";
            case "spawners" -> "Spawners";
            case "chunks-gerados-por-segundo" -> "Chunks gerados";
            case "entidades" -> "Entidades";
            case "itens" -> "Itens no chão";
            case "xp-orbes" -> "Orbes de XP";
            default -> key;
        };
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
        return plugin.getConfigManager().getFixedLimit(player.getWorld(), key, 0);
    }

    private int currentCreatureLimit(Player player, String key) {
        return plugin.getConfigManager().getMobLimit(player.getWorld(), mobLimitKey(key), 8);
    }

    private String currentRadius(Player player, String key) {
        double radius = plugin.getConfigManager().getMobRadius(player.getWorld(), mobLimitKey(key), 16);
        return radius == Math.rint(radius)
                ? String.valueOf((int) radius)
                : String.format(java.util.Locale.ROOT, "%.1f", radius);
    }

    private String mobLimitKey(String key) {
        return key.equals("charged_creeper") ? "creeper" : key;
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
            case "catalisadores-de-sculk" -> Material.SCULK_CATALYST;
            case "emissores-de-sculk" -> Material.SCULK_SHRIEKER;
            case "sinalizadores" -> Material.BEACON;
            case "pistoes-com-slime" -> Material.STICKY_PISTON;
            case "carrinho-com-dinamite" -> Material.TNT_MINECART;
            case "spawners" -> Material.SPAWNER;
            case "chunks-gerados-por-segundo" -> Material.MAP;
            case "pistoes-ativacoes-por-segundo" -> Material.PISTON;
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
            case "strider" -> "Lavagante"; case "charged_creeper" -> "Creeper eletrificado"; case "wither" -> "Wither"; default -> mob;
        };
    }
    private int limitBackSlot(String pageKey, int maxPage) {
        if (maxPage > 0) return CREATURE_BACK_SLOT;
        int size = guiInt("paginas." + pageKey, "tamanho-pagina-unica", SINGLE_CREATURE_PAGE_SIZE);
        return size <= OTHER_CREATURE_PAGE_SIZE ? CREATURE_PREVIOUS_SLOT : SINGLE_CREATURE_BACK_SLOT;
    }

    private String stripColors(String text) {
        return text.replaceAll("(?i)&[0-9A-FK-OR]", "");
    }

    private String color(String text) {
        return text.replace('&', '§');
    }

    private boolean isMainTitle(String title) {
        return color(mainTitle()).equals(title);
    }

    private boolean isCategoryTitle(String title) {
        return color(guiString("criaturas", "titulo", "&7Limites &8• &7Criaturas &8• &7Página 1&8/&71")).equals(title);
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

            if (page == 0 && slot == (maxPage > 0 ? CREATURE_BACK_SLOT : SINGLE_CREATURE_BACK_SLOT)) {
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

        String limitPage = findLimitPageByTitle(title);
        if (limitPage == null) return;

        List<String> keys = limitPageKeys(limitPage);
        int page = extrairPagina(title);
        int maxPage = Math.max(0, (keys.size() - 1) / HEAD_SLOTS.length);

        if (page == 0 && slot == limitBackSlot(limitPage, maxPage)) {
            openMain(player);
            return;
        }
        if (page > 0 && slot == CREATURE_PREVIOUS_SLOT) {
            openLimitPage(player, limitPage, keys, page - 1);
            return;
        }
        if (page < maxPage && slot == CREATURE_NEXT_SLOT) {
            openLimitPage(player, limitPage, keys, page + 1);
        }
    }

    private int extrairPagina(String title) {
        String plainTitle = title.replaceAll("(?i)§[0-9a-fk-or]", "");
        int separator = plainTitle.lastIndexOf(" • ");
        if (separator < 0) return 0;
        String pagePart = plainTitle.substring(separator + 3);
        int slash = pagePart.indexOf('/');
        if (slash < 0) return 0;
        String number = pagePart.substring(0, slash).replaceAll("\\D+", "");
        if (number.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(number) - 1);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String findLimitPageByTitle(String title) {
        if (title == null) return null;
        for (String page : List.of("redstone", "geradores")) {
            String configuredTitle = color(guiString("paginas." + page, "titulo", "&8Limites"));
            if (title.startsWith(configuredTitle)) return page;
        }
        return null;
    }

    private List<String> limitPageKeys(String page) {
        return switch (page) {
            case "redstone" -> REDSTONE_KEYS;
            case "geradores" -> GENERATOR_KEYS;
            default -> List.of();
        };
    }

    private boolean isLimitsInventory(String title) {
        if (title == null) return false;
        return isMainTitle(title) || isCategoryTitle(title)
                || findLimitPageByTitle(title) != null
                || isCreaturePageTitle(title);
    }

    private record Category(String key, Material material) {}

}