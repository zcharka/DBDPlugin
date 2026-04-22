package pl.dbd.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.dbd.DBDPlugin;

import java.util.ArrayList;
import java.util.List;

public class ShopManager implements Listener {
    private final DBDPlugin plugin;
    // Lista korzysta teraz z pl.dbd.shop.ShopItem (z osobnego pliku)
    private final List<ShopItem> items = new ArrayList<>();

    public ShopManager(DBDPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadItems();
    }

    private void loadItems() {
        items.add(new ShopItem("latarka", "Latarka", Material.TORCH, 500));
        items.add(new ShopItem("apteczka", "Apteczka", Material.RED_DYE, 400));
        items.add(new ShopItem("klucz", "Klucz", Material.TRIPWIRE_HOOK, 2000));
        items.add(new ShopItem("sprint", "Perk: Sprint", Material.FEATHER, 1500));
    }

    public void openShop(Player p) {
        // Przekazujemy otwarcie do GUI
        new ShopGUI(plugin).open(p);
    }
    
    public List<ShopItem> getItems() {
        return items;
    }
    
    public ShopItem getItem(String id) {
        for (ShopItem i : items) {
             if (i.getId().equalsIgnoreCase(id)) return i; 
        }
        return null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // Pusty handler, bo GUI ma swój własny w klasie ShopGUI
    }
}