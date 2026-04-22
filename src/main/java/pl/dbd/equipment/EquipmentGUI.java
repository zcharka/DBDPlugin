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
// WAŻNE: Importujemy klasę z osobnego pliku, żeby naprawić błędy "incompatible types"
import pl.dbd.equipment.EqEntry;

import java.util.ArrayList;
import java.util.List;

public class EquipmentGUI implements Listener {

    private final DBDPlugin plugin;
    private final Player player;

    // Konstruktor przyjmujący gracza (naprawia błąd "constructor cannot be applied")
    public EquipmentGUI(DBDPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        // Rejestrujemy listenera, żeby to GUI reagowało na kliknięcia
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        // Tworzymy inventory 54 sloty (duża skrzynia)
        Inventory inv = Bukkit.createInventory(null, 54, "§8§lEkwipunek DBD");

        // Wypełnienie tła szarym szkłem
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta();
        bm.setDisplayName(" ");
        bg.setItemMeta(bm);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        EquipmentManager manager = plugin.getEquipmentManager();
        
        // Sprawdzamy rolę gracza (czy jest Killerem czy Survivorem)
        EqEntry.Role role = EqEntry.Role.SURVIVOR;
        if (plugin.getGameManager().isKiller(player)) {
            role = EqEntry.Role.KILLER;
        }

        // --- SEKCJA 1: PRZEDMIOTY (Góra) ---
        List<EqEntry> items = manager.getAvailable(role, EqEntry.Type.ITEM);
        int slot = 10;
        
        for (EqEntry entry : items) {
            String equippedId = manager.getEquippedItem(player, role);
            boolean isEquipped = entry.getId().equals(equippedId);

            ItemStack icon = new ItemStack(entry.getMaterial());
            ItemMeta meta = icon.getItemMeta();
            
            // Formatowanie nazwy w zależności od statusu
            if (isEquipped) {
                meta.setDisplayName("§a§l[WYBRANO] §e" + entry.getDisplayName());
            } else {
                meta.setDisplayName("§c" + entry.getDisplayName());
            }
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + entry.getDescription());
            lore.add("§7Koszt: §6" + entry.getCost());
            lore.add(" ");
            lore.add(isEquipped ? "§aJuż posiadasz ten przedmiot." : "§eKliknij lewym, aby wybrać.");
            
            meta.setLore(lore);
            icon.setItemMeta(meta);
            
            // Ustawiamy w slocie (o ile mieści się w linii)
            if (slot < 17) inv.setItem(slot++, icon);
        }

        // --- SEKCJA 2: PERKI (Dół) ---
        List<EqEntry> perks = manager.getAvailable(role, EqEntry.Type.PERK);
        slot = 28;
        List<String> equippedPerks = manager.getEquippedPerks(player, role);

        for (EqEntry entry : perks) {
            boolean isEquipped = equippedPerks.contains(entry.getId());

            ItemStack icon = new ItemStack(entry.getMaterial());
            ItemMeta meta = icon.getItemMeta();
            
            if (isEquipped) {
                meta.setDisplayName("§b§l[PERK AKTYWNY] §f" + entry.getDisplayName());
            } else {
                meta.setDisplayName("§7" + entry.getDisplayName());
            }
            
            List<String> lore = new ArrayList<>();
            lore.add("§7" + entry.getDescription());
            lore.add("§7Koszt: §6" + entry.getCost());
            lore.add(" ");
            lore.add(isEquipped ? "§cKliknij, aby dezaktywować." : "§aKliknij, aby aktywować.");
            
            meta.setLore(lore);
            icon.setItemMeta(meta);

            // Ustawianie perków w kilku rzędach
            if (slot < 44) {
                inv.setItem(slot++, icon);
                // Jeśli koniec rzędu (slot 35), przeskocz do następnego (slot 37)
                if ((slot + 1) % 9 == 0) slot += 2;
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // Sprawdzamy czy to nasze GUI
        if (!e.getView().getTitle().equals("§8§lEkwipunek DBD")) return;
        
        e.setCancelled(true); // Blokujemy wyciąganie itemów
        
        if (e.getWhoClicked() != player) return; // Reagujemy tylko na właściciela GUI
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        if (e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        EquipmentManager manager = plugin.getEquipmentManager();
        String displayName = e.getCurrentItem().getItemMeta().getDisplayName();
        
        // Określamy rolę gracza
        EqEntry.Role role = EqEntry.Role.SURVIVOR;
        if (plugin.getGameManager().isKiller(player)) role = EqEntry.Role.KILLER;

        // "Czyścimy" nazwę klikniętego przedmiotu z kolorów i tagów, żeby znaleźć oryginał
        String cleanName = displayName
                .replace("§a§l[WYBRANO] §e", "")
                .replace("§c", "")
                .replace("§b§l[PERK AKTYWNY] §f", "")
                .replace("§7", "");

        // Szukamy pasującego wpisu w bazie danych managera
        EqEntry clickedEntry = null;
        List<EqEntry> allAvailable = new ArrayList<>();
        allAvailable.addAll(manager.getAvailable(role, EqEntry.Type.ITEM));
        allAvailable.addAll(manager.getAvailable(role, EqEntry.Type.PERK));

        for (EqEntry entry : allAvailable) {
            if (entry.getDisplayName().equals(cleanName)) {
                clickedEntry = entry;
                break;
            }
        }

        // Jeśli znaleziono przedmiot, wykonujemy akcję
        if (clickedEntry != null) {
            if (clickedEntry.getType() == EqEntry.Type.ITEM) {
                // Ustawiamy przedmiot
                manager.setEquippedItem(player, role, clickedEntry.getId());
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
                player.sendMessage("§aWybrano przedmiot: " + clickedEntry.getDisplayName());
            } 
            else if (clickedEntry.getType() == EqEntry.Type.PERK) {
                // Włączamy/Wyłączamy perk
                manager.togglePerk(player, role, clickedEntry.getId());
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            
            // Odświeżamy GUI, żeby pokazać zmiany (zielone ramki itp.)
            open();
        }
    }
}