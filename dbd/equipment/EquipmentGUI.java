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

    private static final String T_MAIN = "§8§lEkwipunek §8» §fMenu Główne";
    private static final String T_S_ITEMS = "§2§lPrzedmioty §8» §aSurvivor";
    private static final String T_S_PERKS = "§2§lPerki §8» §aSurvivor";
    private static final String T_K_ITEMS = "§c§lKiller: Przedmioty";
    private static final String T_K_PERKS = "§5§lKiller: Perki";
    private static final String T_KILLERS = "§4§lWybór Zabójcy";

    private final DBDPlugin plugin;

    public EquipmentGUI(DBDPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ── GŁÓWNE MENU ─────────────────────────────────────────────────────────────
    public void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 45, T_MAIN);

        ItemStack bgSurv = item(Material.LIME_STAINED_GLASS_PANE, " ");
        ItemStack bgKill = item(Material.RED_STAINED_GLASS_PANE, " ");
        ItemStack bgMid = item(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Rysowanie tła: Lewa strona zielona, prawa czerwona, środek szary
        for (int i = 0; i < 45; i++) {
            int col = i % 9;
            if (col < 4)
                inv.setItem(i, bgSurv);
            else if (col > 4)
                inv.setItem(i, bgKill);
            else
                inv.setItem(i, bgMid);
        }

        // --- SEKCJA SURVIVORA (Lewa) ---
        inv.setItem(11, itemLore(Material.CHEST, "§a§lPrzedmioty Ocalałego", "§7Kliknij, aby wybrać przedmiot"));
        inv.setItem(20, itemLore(Material.ENCHANTING_TABLE, "§a§lPerki Ocalałego", "§7Kliknij, aby wybrać perki",
                "§8(Max 4 aktywne)"));

        // --- SEKCJA KILLERA (Prawa) ---
        inv.setItem(15, itemLore(Material.ENDER_CHEST, "§c§lPrzedmioty Zabójcy", "§7Kliknij, aby wybrać przedmiot"));
        inv.setItem(24,
                itemLore(Material.BOOK, "§c§lPerki Zabójcy", "§7Kliknij, aby wybrać perki", "§8(Max 4 aktywne)"));

        // --- WYBÓR KILLERA (Slot 32 - prawy dolny róg sekcji killera) ---
        String equippedKiller = plugin.getEquipmentManager().getEquippedKiller(p);
        String killerInfo = equippedKiller != null ? "§7Wybrany: §c" + equippedKiller : "§7Brak wybranego zabójcy";
        inv.setItem(33, itemLore(Material.WITHER_SKELETON_SKULL, "§4§lWybór Zabójcy", killerInfo,
                "§7Kliknij, aby wybrać postać Killera"));

        // --- INNE (Środek/Dół) ---
        long souls = plugin.getSoulsManager().getBalance(p);
        inv.setItem(4, itemLore(Material.SUNFLOWER, "§6§lDusze Nexusa", "§7Twoje saldo: §e" + souls));

        p.openInventory(inv);
    }

    // ── POD-MENU (Przedmioty, Perki, Klucze) ───────────────────────────────────
    private void openSubMenu(Player p, String title, EqEntry.Role role, EqEntry.Type type) {
        Inventory inv = Bukkit.createInventory(null, 54, title);

        Material borderMat = role == EqEntry.Role.KILLER ? Material.RED_STAINED_GLASS_PANE
                : Material.LIME_STAINED_GLASS_PANE;
        ItemStack border = item(borderMat, " ");
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        inv.setItem(4, itemLore(Material.SPECTRAL_ARROW, "§c§lWróć do Menu", "§7Kliknij, aby cofnąć."));

        EquipmentManager mgr = plugin.getEquipmentManager();

        if (type == EqEntry.Type.ITEM) {
            List<EqEntry> items = mgr.getOwnedItems(p, role);
            String equipped = mgr.getEquippedItem(p, role);
            placeEntries(inv, items, 9, equipped, null);

            inv.setItem(49, itemLore(Material.PAPER, "§e§lInformacja",
                    "§7Możesz mieć założony", "§e§ltylko 1 przedmiot", "§7na raz!"));
        } else {
            List<EqEntry> perks = mgr.getUnlockedPerks(p, role);
            List<String> equipped = mgr.getEquippedPerks(p, role);
            placeEntries(inv, perks, 9, null, equipped);

            inv.setItem(49, itemLore(Material.BOOK, "§dTwój Limit", "§7Wyposażono: §e" + equipped.size() + "§8/4"));
        }

        p.openInventory(inv);
    }

    // ── POD-MENU KILLERÓW ────────────────────────────────────────────────────────
    private void openKillersMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, T_KILLERS);

        ItemStack border = item(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++)
            inv.setItem(i, border);
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);

        inv.setItem(4, itemLore(Material.SPECTRAL_ARROW, "§c§lWróć do Menu", "§7Kliknij, aby cofnąć."));

        EquipmentManager mgr = plugin.getEquipmentManager();
        List<EqEntry> killers = mgr.getOwnedKillers(p);
        String equipped = mgr.getEquippedKiller(p);
        placeEntries(inv, killers, 9, equipped, null);

        inv.setItem(49, itemLore(Material.WITHER_SKELETON_SKULL, "§e§lInformacja",
                "§7Możesz mieć wybranego", "§e§ltylko 1 zabójcę", "§7na raz!"));

        p.openInventory(inv);
    }

    // ── LOGIKA KLIKNIĘĆ ────────────────────────────────────────────────────────
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        String title = e.getView().getTitle();

        if (!title.equals(T_MAIN) && !title.equals(T_S_ITEMS) && !title.equals(T_S_PERKS)
                && !title.equals(T_K_ITEMS) && !title.equals(T_K_PERKS) && !title.equals(T_KILLERS))
            return;

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize())
            return;

        // Powrót do menu
        if (slot == 4 && !title.equals(T_MAIN)) {
            openMainMenu(p);
            return;
        }

        // Kliknięcia w Menu Głównym
        if (title.equals(T_MAIN)) {
            if (slot == 11)
                openSubMenu(p, T_S_ITEMS, EqEntry.Role.SURVIVOR, EqEntry.Type.ITEM);
            if (slot == 20)
                openSubMenu(p, T_S_PERKS, EqEntry.Role.SURVIVOR, EqEntry.Type.PERK);
            if (slot == 15)
                openSubMenu(p, T_K_ITEMS, EqEntry.Role.KILLER, EqEntry.Type.ITEM);
            if (slot == 24)
                openSubMenu(p, T_K_PERKS, EqEntry.Role.KILLER, EqEntry.Type.PERK);
            if (slot == 33)
                openKillersMenu(p);
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE"))
            return;

        // Wyciąganie ID z lore
        String id = null;
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            for (String line : clicked.getItemMeta().getLore()) {
                if (line.startsWith("§8ID: ")) {
                    id = line.substring(6);
                    break;
                }
            }
        }
        if (id == null)
            return;

        EquipmentManager mgr = plugin.getEquipmentManager();

        // Akcje w podmenu
        if (title.equals(T_S_ITEMS)) {
            mgr.toggleEquippedItem(p, EqEntry.Role.SURVIVOR, id);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            openSubMenu(p, title, EqEntry.Role.SURVIVOR, EqEntry.Type.ITEM);
        } else if (title.equals(T_S_PERKS)) {
            mgr.toggleEquippedPerk(p, EqEntry.Role.SURVIVOR, id);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openSubMenu(p, title, EqEntry.Role.SURVIVOR, EqEntry.Type.PERK);
        } else if (title.equals(T_K_ITEMS)) {
            mgr.toggleEquippedItem(p, EqEntry.Role.KILLER, id);
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            openSubMenu(p, title, EqEntry.Role.KILLER, EqEntry.Type.ITEM);
        } else if (title.equals(T_K_PERKS)) {
            mgr.toggleEquippedPerk(p, EqEntry.Role.KILLER, id);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openSubMenu(p, title, EqEntry.Role.KILLER, EqEntry.Type.PERK);
        } else if (title.equals(T_KILLERS)) {
            mgr.toggleEquippedKiller(p, id);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.2f);
            openKillersMenu(p);
        }
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────────
    private void placeEntries(Inventory inv, List<EqEntry> entries, int startSlot, String equippedItem,
            List<String> equippedPerks) {
        int slot = startSlot;
        for (EqEntry e : entries) {
            if (slot >= 45)
                break;

            boolean isEquipped = false;
            if (equippedItem != null)
                isEquipped = equippedItem.equals(e.id());
            if (equippedPerks != null)
                isEquipped = equippedPerks.contains(e.id());

            String status = isEquipped ? "§a§l[WYPOSAŻONO] " : "§7";
            List<String> lore = new ArrayList<>(e.desc());

            if (e.consumable()) {
                int amount = plugin.getEquipmentManager()
                        .getEqAmount(plugin.getServer().getPlayer(inv.getViewers().get(0).getName()), e.id());
                lore.add("");
                lore.add("§8Ilość sztuk: §e" + amount);
                lore.add("§c(Jednorazowy - zużywa się po wejściu do meczu)");
            } else if (e.type() == EqEntry.Type.ITEM) {
                lore.add("");
                lore.add("§a(Wielorazowy - nie znika po meczu)");
            }

            lore.add("");
            lore.add(isEquipped ? "§cKliknij, aby ZDJĄĆ" : "§aKliknij, aby ZAŁOŻYĆ");
            lore.add("§8ID: " + e.id());

            inv.setItem(slot++, itemLore(e.material(), status + e.display(), lore.toArray(new String[0])));
        }
    }

    private ItemStack item(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        i.setItemMeta(m);
        return i;
    }

    private ItemStack itemLore(Material mat, String name, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        i.setItemMeta(m);
        return i;
    }
}