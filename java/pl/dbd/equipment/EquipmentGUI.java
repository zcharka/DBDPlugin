package pl.dbd.equipment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.dbd.DBDPlugin;

import java.util.*;

public class EquipmentGUI implements Listener {

    private final DBDPlugin plugin;

    public static class PlayerGUIState {
        public enum MenuType { SURV_PERKS, SURV_ITEMS, KILL_PERKS }
        public MenuType menuType = MenuType.SURV_PERKS;
        public int page = 1;
    }

    private final Map<UUID, PlayerGUIState> states = new HashMap<>();

    public EquipmentGUI(DBDPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player p) {
        openSurvivorPerks(p);
    }

    public void openSurvivorPerks(Player p) {
        PlayerGUIState state = states.computeIfAbsent(p.getUniqueId(), k -> new PlayerGUIState());
        state.menuType = PlayerGUIState.MenuType.SURV_PERKS;
        state.page = 1;
        openGUI(p, state);
    }

    public void openSurvivorItems(Player p) {
        PlayerGUIState state = states.computeIfAbsent(p.getUniqueId(), k -> new PlayerGUIState());
        state.menuType = PlayerGUIState.MenuType.SURV_ITEMS;
        state.page = 1;
        openGUI(p, state);
    }

    public void openKillerPerks(Player p) {
        PlayerGUIState state = states.computeIfAbsent(p.getUniqueId(), k -> new PlayerGUIState());
        state.menuType = PlayerGUIState.MenuType.KILL_PERKS;
        state.page = 1;
        openGUI(p, state);
    }

    public void openKillersMenu(Player p) {
        p.closeInventory();
        p.performCommand("dbdwyborkillera");
    }

    public void openUpgradeMenu(Player p) {
        p.closeInventory();
        p.performCommand("ulepsz");
    }

    // Adapter dla starych metod
    public void openSubMenu(Player p, String title, EqEntry.Role role, EqEntry.Type type) {
        if (role == EqEntry.Role.SURVIVOR && type == EqEntry.Type.PERK) openSurvivorPerks(p);
        else if (role == EqEntry.Role.SURVIVOR && type == EqEntry.Type.ITEM) openSurvivorItems(p);
        else if (role == EqEntry.Role.KILLER && type == EqEntry.Type.PERK) openKillerPerks(p);
        else openSurvivorPerks(p);
    }

    public void openGUI(Player p, PlayerGUIState state) {
        String invTitle = EquipmentManager.color("&8Ekwipunek u <##003BFF>&lᴛ<##0443FF>&lᴇ<##084AFE>&lᴄ<##0C52FE>&lʜ<##1059FD>&lɴ<##1461FD>&lɪ<##1868FC>&lᴋ &f_sebZi_");
        Inventory inv = Bukkit.createInventory(null, 45, invTitle);

        // 1. Tło
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
        }

        int[] brown = {0, 1, 7, 8, 9, 17, 27, 35, 36, 37, 43, 44};
        for (int b : brown) inv.setItem(b, item(Material.BROWN_STAINED_GLASS_PANE, " "));

        int[] dark = {2, 3, 4, 5, 6, 18, 26};
        for (int d : dark) inv.setItem(d, item(Material.GRAY_STAINED_GLASS_PANE, " "));

        // 2. Ładowanie itemów i stron
        EquipmentManager mgr = plugin.getEquipmentManager();
        List<EqEntry> entries;
        List<String> equipped;
        String equippedSingle = null;
        int[] slots;

        if (state.menuType == PlayerGUIState.MenuType.SURV_PERKS) {
            entries = mgr.getUnlockedPerks(p, EqEntry.Role.SURVIVOR);
            equipped = mgr.getEquippedPerks(p, EqEntry.Role.SURVIVOR);
            slots = new int[]{20, 21, 22, 23, 24};
        } else if (state.menuType == PlayerGUIState.MenuType.KILL_PERKS) {
            entries = mgr.getUnlockedPerks(p, EqEntry.Role.KILLER);
            equipped = mgr.getEquippedPerks(p, EqEntry.Role.KILLER);
            slots = new int[]{20, 21, 22, 23, 24};
        } else {
            entries = mgr.getOwnedItems(p, EqEntry.Role.SURVIVOR);
            equippedSingle = mgr.getEquippedItem(p, EqEntry.Role.SURVIVOR);
            equipped = new ArrayList<>();
            if (equippedSingle != null) equipped.add(equippedSingle);
            slots = new int[]{20, 21, 23, 24};
        }

        int maxPages = (int) Math.ceil((double) entries.size() / slots.length);
        if (maxPages == 0) maxPages = 1;
        if (state.page > maxPages) state.page = maxPages;
        if (state.page < 1) state.page = 1;

        int startIndex = (state.page - 1) * slots.length;
        int endIndex = Math.min(startIndex + slots.length, entries.size());

        for (int i = startIndex; i < endIndex; i++) {
            EqEntry entry = entries.get(i);
            int slot = slots[i - startIndex];
            
            boolean isEquipped = equipped.contains(entry.id());
            String status = isEquipped ? "§a§l[WYPOSAŻONO] " : "§7";
            List<String> lore = new ArrayList<>();
            if (entry.desc() != null) lore.addAll(entry.desc());

            if (entry.consumable()) {
                int amount = plugin.getEquipmentManager().getEqAmount(p, entry.id());
                lore.add("");
                lore.add("§8Ilość sztuk: §e" + amount);
                lore.add("§c(Jednorazowy - zużywa się po wejściu do meczu)");
            } else if (entry.type() == EqEntry.Type.ITEM) {
                lore.add("");
                lore.add("§a(Wielorazowy - nie znika po meczu)");
            }

            lore.add("");
            lore.add(isEquipped ? "§cKliknij, aby ZDJĄĆ" : "§aKliknij, aby ZAŁOŻYĆ");
            lore.add("§8ID: " + entry.id());

            inv.setItem(slot, itemLore(entry.material(), status + entry.display(), lore.toArray(new String[0])));
        }

        // 3. Strony (strzałki)
        if (state.page > 1) {
            inv.setItem(18, item(Material.ARROW, "§ePoprzednia strona"));
        }
        if (state.page < maxPages) {
            inv.setItem(26, item(Material.ARROW, "§eNastępna strona"));
        }

        // 4. Dolny pasek nawigacyjny
        int survPerksSize = mgr.getEquippedPerks(p, EqEntry.Role.SURVIVOR).size();
        int killPerksSize = mgr.getEquippedPerks(p, EqEntry.Role.KILLER).size();
        long souls = plugin.getSoulsManager().getBalance(p);

        ItemStack btnSurvPerks = itemLore(Material.SOUL_LANTERN, "§f§lPerki Ocalałych", 
            "§7Kliknij, aby otworzyć!", "", "§d§lTwój Limit: §e" + survPerksSize + "§8/§e4");
        if (state.menuType == PlayerGUIState.MenuType.SURV_PERKS) btnSurvPerks = activeItem(btnSurvPerks);

        ItemStack btnSurvItems = itemLore(Material.RABBIT_HIDE, "§f§lPrzedmioty Ocalałych",
            "§7Kliknij, aby otworzyć!");
        if (state.menuType == PlayerGUIState.MenuType.SURV_ITEMS) btnSurvItems = activeItem(btnSurvItems);

        ItemStack btnKillPerks = itemLore(Material.BLAZE_POWDER, "§f§lPerki Zabójców",
            "§7Kliknij, aby otworzyć!", "", "§d§lTwój Limit: §e" + killPerksSize + "§8/§e4");
        if (state.menuType == PlayerGUIState.MenuType.KILL_PERKS) btnKillPerks = activeItem(btnKillPerks);

        ItemStack btnKillers = itemLore(Material.WITHER_SKELETON_SKULL, "§f§lZabójcy",
            "§7Kliknij, aby wybrać zabójcę!");

        ItemStack btnUpgrade = itemLore(Material.ANVIL, "§f§lUlepszanie",
            "§7Kliknij, aby przejść do ulepszania!");

        inv.setItem(38, btnSurvPerks);
        inv.setItem(39, btnSurvItems);
        inv.setItem(40, btnKillPerks);
        inv.setItem(41, btnKillers);
        inv.setItem(42, btnUpgrade);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (!title.startsWith("§8Ekwipunek u ")) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        PlayerGUIState state = states.get(p.getUniqueId());
        if (state == null) return;

        // Klawisze nawigacyjne na dole
        if (slot == 38) {
            if (state.menuType != PlayerGUIState.MenuType.SURV_PERKS) {
                state.menuType = PlayerGUIState.MenuType.SURV_PERKS;
                state.page = 1;
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openGUI(p, state);
            }
            return;
        }
        if (slot == 39) {
            if (state.menuType != PlayerGUIState.MenuType.SURV_ITEMS) {
                state.menuType = PlayerGUIState.MenuType.SURV_ITEMS;
                state.page = 1;
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openGUI(p, state);
            }
            return;
        }
        if (slot == 40) {
            if (state.menuType != PlayerGUIState.MenuType.KILL_PERKS) {
                state.menuType = PlayerGUIState.MenuType.KILL_PERKS;
                state.page = 1;
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                openGUI(p, state);
            }
            return;
        }
        if (slot == 41) {
            p.closeInventory();
            p.performCommand("dbdwyborkillera");
            return;
        }
        if (slot == 42) {
            p.closeInventory();
            p.performCommand("ulepsz");
            return;
        }

        // Strony
        if (slot == 18 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
            state.page--;
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            openGUI(p, state);
            return;
        }
        if (slot == 26 && e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
            state.page++;
            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            openGUI(p, state);
            return;
        }

        // Kliknięcia w przedmioty
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE")) return;

        String id = null;
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            for (String line : clicked.getItemMeta().getLore()) {
                if (line.startsWith("§8ID: ")) {
                    id = line.substring(6);
                    break;
                }
            }
        }
        if (id == null) return;

        EquipmentManager mgr = plugin.getEquipmentManager();
        if (state.menuType == PlayerGUIState.MenuType.SURV_PERKS) {
            mgr.toggleEquippedPerk(p, EqEntry.Role.SURVIVOR, id);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openGUI(p, state);
        } else if (state.menuType == PlayerGUIState.MenuType.SURV_ITEMS) {
            mgr.toggleEquippedItem(p, EqEntry.Role.SURVIVOR, id);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            openGUI(p, state);
        } else if (state.menuType == PlayerGUIState.MenuType.KILL_PERKS) {
            mgr.toggleEquippedPerk(p, EqEntry.Role.KILLER, id);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openGUI(p, state);
        }
    }

    private ItemStack item(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack itemLore(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            m.setLore(Arrays.asList(lore));
            i.setItemMeta(m);
        }
        return i;
    }

    private ItemStack activeItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}