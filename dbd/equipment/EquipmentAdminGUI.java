package pl.dbd.equipment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.dbd.DBDPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EquipmentAdminGUI implements Listener, InventoryHolder {

    private final DBDPlugin plugin;
    private static final Map<UUID, Integer> pages = new HashMap<>();

    public EquipmentAdminGUI(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null; // Zapobiega błędom
    }

    public void open(Player p) {
        open(p, 1);
    }

    public void open(Player p, int page) {
        pages.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(this, 54, "§c§lADMIN: Edycja Eq §8[" + page + "]");

        EquipmentManager eqMngr = plugin.getEquipmentManager();
        List<String> ids = new ArrayList<>(eqMngr.getAllRegisteredIds());

        int maxPages = (int) Math.ceil((double) ids.size() / 45.0);
        if (maxPages == 0)
            maxPages = 1;

        int slot = 0;
        int startIndex = (page - 1) * 45;

        for (int i = startIndex; i < ids.size() && slot < 45; i++) {
            String id = ids.get(i);
            EqEntry entry = eqMngr.getEntry(id);
            if (entry == null)
                continue;

            ItemStack is = new ItemStack(
                    entry.material() != null && entry.material() != Material.AIR ? entry.material() : Material.PAPER);
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(entry.display());
                List<String> lore = new ArrayList<>();
                if (entry.desc() != null) {
                    lore.addAll(entry.desc());
                }
                lore.add("");
                lore.add("§8---------------");
                lore.add("§7Rola: §e" + entry.role().name());
                lore.add("§7Typ: §b" + entry.type().name());
                lore.add("§7Jednorazowy: " + (entry.consumable() ? "§aTAK" : "§cNIE"));
                lore.add("§8---------------");
                lore.add("§eLewy Klik §8→ §fZmień Rolę");
                lore.add("§6Prawy Klik §8→ §fZmień Typ");
                lore.add("§dŚrodkowy Klik §8→ §fZmień Konsumowalność");
                lore.add("§cShift+LPM §8→ §fUsuń z puli");
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }

        // Przyciski nawigacyjne
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            if (pm != null) {
                pm.setDisplayName("§ePoprzednia strona");
                prev.setItemMeta(pm);
            }
            inv.setItem(48, prev);
        }

        if (page < maxPages) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            if (nm != null) {
                nm.setDisplayName("§eNastępna strona");
                next.setItemMeta(nm);
            }
            inv.setItem(50, next);
        }

        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof EquipmentAdminGUI))
            return;

        e.setCancelled(true);
        e.setResult(Event.Result.DENY);

        Player p = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null)
            return;

        EquipmentManager eqMngr = plugin.getEquipmentManager();

        // Dodawanie z eq gracza
        if (clickedInv.equals(e.getView().getBottomInventory())) {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR)
                return;

            String id = "eq_" + UUID.randomUUID().toString().substring(0, 6);
            eqMngr.addGuiEntryFromAdmin(id, clickedItem);

            p.sendMessage(pl.dbd.DBDPlugin.getMsg("alsukces7dodanonowyp1"));
            open(p); // odśwież
            return;
        }

        // Edycja górnego inventory
        if (clickedInv.equals(e.getView().getTopInventory())) {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR)
                return;

            int currentPage = pages.getOrDefault(p.getUniqueId(), 1);

            if (e.getRawSlot() == 48 && clickedItem.getType() == Material.ARROW) {
                open(p, currentPage - 1);
                return;
            } else if (e.getRawSlot() == 50 && clickedItem.getType() == Material.ARROW) {
                open(p, currentPage + 1);
                return;
            }

            // Szukamy ID na podstawie slotu/indexu (z uwzględnieniem strony)
            int index = (currentPage - 1) * 45 + e.getRawSlot();
            List<String> ids = new ArrayList<>(eqMngr.getAllRegisteredIds());
            if (index < 0 || index >= ids.size())
                return;

            String targetId = ids.get(index);

            EqEntry entry = eqMngr.getEntry(targetId);
            if (entry == null)
                return;

            if (e.getClick() == ClickType.SHIFT_LEFT) {
                eqMngr.removeGuiEntry(targetId);
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cusunitozbazequipmen"));
                open(p, currentPage);
                return;
            }

            if (e.isLeftClick()) {
                eqMngr.toggleGuiRole(targetId);
                open(p, currentPage);
                return;
            }

            if (e.isRightClick() && e.getClick() != ClickType.MIDDLE) {
                eqMngr.toggleGuiType(targetId);
                open(p, currentPage);
                return;
            }

            if (e.getClick() == ClickType.MIDDLE) {
                eqMngr.toggleGuiConsumable(targetId);
                open(p, currentPage);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof EquipmentAdminGUI) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }
    }
}
