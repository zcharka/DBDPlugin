package pl.dbd.shop;

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
import java.util.Arrays;
import java.util.List;

public class ShopGUI implements Listener {

    private final DBDPlugin plugin;

    public ShopGUI(DBDPlugin plugin) {
        this.plugin = plugin;
        // Rejestrujemy listenera, żeby dało się klikać
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lSKLEP DBD (Dusze)");

        // Pobieramy listę przedmiotów (teraz typy się zgadzają idealnie)
        List<ShopItem> items = plugin.getShopManager().getItems();

        int slot = 10;
        for (ShopItem item : items) {
            ItemStack is = new ItemStack(item.getMaterial());
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("§e" + item.getName());
            meta.setLore(Arrays.asList(
                "§7Koszt: §6" + item.getPrice() + " Dusz",
                "§eKliknij, aby kupić!"
            ));
            is.setItemMeta(meta);
            
            if (slot < 26) {
                inv.setItem(slot++, is);
            }
        }

        // Info o duszach
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§eStan konta: §6" + plugin.getSoulsManager().getBalance(p));
        info.setItemMeta(im);
        inv.setItem(26, info);

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§6§lSKLEP DBD (Dusze)")) return;
        e.setCancelled(true);
        
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
        Player p = (Player) e.getWhoClicked();

        String displayName = e.getCurrentItem().getItemMeta().getDisplayName().replace("§e", "");
        
        // Szukamy przedmiotu
        ShopItem clickedItem = null;
        for (ShopItem item : plugin.getShopManager().getItems()) {
            if (item.getName().equals(displayName)) {
                clickedItem = item;
                break;
            }
        }

        if (clickedItem != null) {
            if (plugin.getSoulsManager().take(p, clickedItem.getPrice())) {
                p.getInventory().addItem(new ItemStack(clickedItem.getMaterial()));
                p.sendMessage("§aKupiłeś: " + clickedItem.getName());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                
                // Odśwież widok (aktualizacja dusz)
                open(p);
            } else {
                p.sendMessage("§cBrak wystarczającej liczby dusz!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}