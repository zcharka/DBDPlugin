package pl.dbd.shop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.dbd.DBDPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager {
    private final DBDPlugin plugin;
    private final File shopFile;
    private FileConfiguration config;
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();

    public ShopManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.shopFile = new File(plugin.getDataFolder(), "shop.yml");
        load();
    }

    public void openShop(Player player) {
        new ShopGUI(plugin).open(player);
    }

    public void load() {
        if (!shopFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                shopFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(shopFile);
        shopItems.clear();
        ConfigurationSection sec = config.getConfigurationSection("items");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                int slot = sec.getInt(key + ".slot");
                int price = sec.getInt(key + ".price");
                ItemStack itemStack = sec.getItemStack(key + ".itemStack");

                // Fallback dla starych danych z poprzedniej wersji sklepu
                if (itemStack == null) {
                    itemStack = new ItemStack(Material.valueOf(sec.getString(key + ".material", "PAPER")));
                    org.bukkit.inventory.meta.ItemMeta meta = itemStack.getItemMeta();
                    if (meta != null) {
                        String name = sec.getString(key + ".name");
                        if (name != null)
                            meta.setDisplayName(name);
                        meta.setLore(sec.getStringList(key + ".lore"));
                        itemStack.setItemMeta(meta);
                    }
                }

                shopItems.put(slot, new ShopItem(key, slot, price, itemStack));
            }
        }
    }

    public void save() {
        try {
            config.save(shopFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setItem(int slot, ItemStack is, int price) {
        String id = "item_" + slot;
        config.set("items." + id + ".slot", slot);
        config.set("items." + id + ".price", price);
        config.set("items." + id + ".itemStack", is.clone());

        // Czyszczenie starych danych
        config.set("items." + id + ".material", null);
        config.set("items." + id + ".name", null);
        config.set("items." + id + ".lore", null);
        config.set("items." + id + ".command", null);

        save();
        load();
    }

    public void moveItem(int oldSlot, int newSlot) {
        ShopItem item = shopItems.get(oldSlot);
        if (item == null)
            return;
        config.set("items." + item.id() + ".slot", newSlot);
        save();
        load();
    }

    public void removeItem(int slot) {
        ShopItem item = shopItems.get(slot);
        if (item == null)
            return;
        config.set("items." + item.id(), null);
        save();
        load();
    }

    public void setPrice(int slot, int newPrice) {
        for (String key : config.getConfigurationSection("items").getKeys(false)) {
            if (config.getInt("items." + key + ".slot") == slot) {
                config.set("items." + key + ".price", newPrice);
                save();
                load();
                break;
            }
        }
    }

    public Map<Integer, ShopItem> getItems() {
        return shopItems;
    }

    public record ShopItem(String id, int slot, int price, ItemStack itemStack) {
    }
}