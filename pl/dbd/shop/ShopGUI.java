package pl.dbd.shop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.dbd.DBDPlugin;
import pl.dbd.equipment.EquipmentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopGUI implements Listener, InventoryHolder {
    private final DBDPlugin plugin;

    public ShopGUI(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(this, 27, EquipmentManager.color("&8&lSKLEP &b&lDUSZ NEXUSA"));

        Map<Integer, ShopManager.ShopItem> items = plugin.getShopManager().getItems();

        for (ShopManager.ShopItem item : items.values()) {
            ItemStack is = item.itemStack().clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(EquipmentManager.color("&7Cena: &b" + item.price() + " &7Dusz"));
                lore.add(EquipmentManager.color("&7Twoje dusze: &e" + plugin.getSoulsManager().getBalance(p)));
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(item.slot(), is);
        }

        p.openInventory(inv);
    }

    // PRIORYTET HIGHEST: Upewniamy się, że żaden inny plugin nam tego nie odblokuje
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopGUI))
            return;

        e.setCancelled(true);
        e.setResult(Event.Result.DENY); // Dodatkowe brutalne odrzucenie eventu

        Player p = (Player) e.getWhoClicked();

        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(e.getView().getTopInventory())) {
            return;
        }

        int slot = e.getRawSlot();
        ShopManager.ShopItem item = plugin.getShopManager().getItems().get(slot);
        if (item == null)
            return;

        if (plugin.getSoulsManager().take(p, item.price())) {
            ItemStack toGive = item.itemStack().clone();
            Map<Integer, ItemStack> leftover = p.getInventory().addItem(toGive);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    p.getWorld().dropItem(p.getLocation(), drop);
                }
            }

            String itemName = toGive.hasItemMeta() && toGive.getItemMeta().hasDisplayName()
                    ? toGive.getItemMeta().getDisplayName()
                    : toGive.getType().name();
            p.sendMessage(EquipmentManager.color("&a&lZAKUPIONO! &7Pomyślnie kupiłeś: &f" + itemName));
            p.closeInventory();
        } else {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7niemaszwystarcz"));
            p.closeInventory();
        }
    }

    // BLOKADA PRZECIĄGANIA (Zabezpiecza przed wyciągnięciem itemu za pomocą LPM/PPM
    // Hold)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ShopGUI) {
            e.setCancelled(true);
            e.setResult(Event.Result.DENY);
        }
    }
}