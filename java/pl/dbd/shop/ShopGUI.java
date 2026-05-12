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
        // Tytuł z Hex-Gradientem: KLUCZE
        String title = EquipmentManager.color(
                "\u00a7x\u00a74\u00a73\u00a7F\u00a76\u00a7E\u00a78\u00a7lK\u00a7x\u00a77\u00a7B\u00a7F\u00a73\u00a7E\u00a79\u00a7lL\u00a7x\u00a7B\u00a74\u00a7E\u00a7F\u00a7E\u00a7B\u00a7lU\u00a7x\u00a7E\u00a7C\u00a7E\u00a7C\u00a7E\u00a7C\u00a7lC\u00a7x\u00a7E\u00a7C\u00a7E\u00a7C\u00a7E\u00a7C\u00a7lZE");
        Inventory inv = Bukkit.createInventory(this, 27, title);

        // Obramówka: LIGHT_BLUE na krawędziach
        ItemStack border = new ItemStack(org.bukkit.Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(" ");
            border.setItemMeta(bm);
        }

        // Separatory: BLACK między slotami z towarem
        ItemStack sep = new ItemStack(org.bukkit.Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta sm = sep.getItemMeta();
        if (sm != null) {
            sm.setDisplayName(" ");
            sep.setItemMeta(sm);
        }

        // Górny rząd (0-8) i dolny rząd (18-26) = obramówka
        for (int i = 0; i <= 8; i++)
            inv.setItem(i, border.clone());
        for (int i = 18; i <= 26; i++)
            inv.setItem(i, border.clone());

        // Środkowy rząd - krawędzie (9, 17) = obramówka
        inv.setItem(9, border.clone());
        inv.setItem(17, border.clone());

        // Środkowy rząd - tło pod przedmioty i puste sloty (10-16)
        for (int i = 10; i <= 16; i++) {
            inv.setItem(i, sep.clone());
        }

        // Sloty 10, 12, 14, 16 = przedmioty sklepowe (wstawiane z konfiguracji)
        Map<Integer, ShopManager.ShopItem> items = plugin.getShopManager().getItems();
        for (ShopManager.ShopItem item : items.values()) {
            ItemStack is = item.itemStack().clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(EquipmentManager.color("&2&lCena&f: &6&l" + item.price() + " &fDusz Nexusa"));
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