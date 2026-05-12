package pl.dbd.shop;

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
import pl.dbd.equipment.EquipmentManager;

import java.util.*;

public class ShopAdminGUI implements Listener, InventoryHolder {
    private final DBDPlugin plugin;
    private static final Map<UUID, Integer> movingItems = new HashMap<>();

    public ShopAdminGUI(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(this, 27, EquipmentManager.color("&4&lADMIN: Edycja Sklepu"));

        plugin.getShopManager().getItems().forEach((slot, item) -> {
            ItemStack is = item.itemStack().clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§7Cena: §b" + item.price());
                lore.add("");
                lore.add("§eLPM/PPM §8→ §fCena +/- 50");
                lore.add("§6Kółko myszy §8→ §fPrzenieś przedmiot");
                lore.add("§cShift+LPM §8→ §fUsuń ze sklepu");
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot, is);
        });

        p.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopAdminGUI))
            return;

        e.setCancelled(true);
        e.setResult(Event.Result.DENY);

        Player p = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null)
            return;

        // --- DODAWANIE PRZEDMIOTU ---
        if (clickedInv.equals(e.getView().getBottomInventory())) {
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR)
                return;

            int freeSlot = -1;
            for (int i = 0; i < 27; i++) {
                if (!plugin.getShopManager().getItems().containsKey(i)) {
                    freeSlot = i;
                    break;
                }
            }

            if (freeSlot == -1) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7sklepjestpenyus"));
                return;
            }

            plugin.getShopManager().setItem(freeSlot, clickedItem, 500);
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("alsukces7dodanonowyp"));
            open(p);
            return;
        }

        // --- OBSŁUGA MENU ADMINA ---
        if (clickedInv.equals(e.getView().getTopInventory())) {
            int slot = e.getRawSlot();
            ShopManager.ShopItem item = plugin.getShopManager().getItems().get(slot);

            if (e.getClick() == ClickType.MIDDLE) {
                if (movingItems.containsKey(p.getUniqueId())) {
                    int oldSlot = movingItems.remove(p.getUniqueId());
                    plugin.getShopManager().moveItem(oldSlot, slot);
                    p.sendMessage(pl.dbd.DBDPlugin.getMsg("aprzeniesionoprzedmi"));
                } else if (item != null) {
                    movingItems.put(p.getUniqueId(), slot);
                    p.sendMessage(pl.dbd.DBDPlugin.getMsg("epodnioseprzedmiotkl"));
                }
                open(p);
                return;
            }

            if (item == null)
                return;

            if (e.getClick() == ClickType.SHIFT_LEFT) {
                plugin.getShopManager().removeItem(slot);
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cusunitoprzedmiotzes"));
                open(p);
                return;
            }

            int newPrice = item.price();
            if (e.isLeftClick() && !e.isShiftClick())
                newPrice += 50;
            if (e.isRightClick() && !e.isShiftClick())
                newPrice = Math.max(0, newPrice - 50);

            if (newPrice != item.price()) {
                plugin.getShopManager().setPrice(slot, newPrice);
                open(p);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ShopAdminGUI) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }
    }
}