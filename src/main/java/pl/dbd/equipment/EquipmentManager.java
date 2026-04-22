package pl.dbd.equipment;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.dbd.DBDPlugin;

import java.io.File;
import java.util.*;

public class EquipmentManager implements Listener {
    private final DBDPlugin plugin;
    private final List<EqEntry> entries = new ArrayList<>();
    private final File eqFile;
    private FileConfiguration eqConfig;

    // Przechowywanie wyborów
    private final Map<UUID, Map<EqEntry.Role, String>> equippedItems = new HashMap<>();
    private final Map<UUID, Map<EqEntry.Role, List<String>>> equippedPerks = new HashMap<>();

    public EquipmentManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.eqFile = new File(plugin.getDataFolder(), "equipment.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadConfig();
        loadEntries();
    }

    private void loadConfig() {
        if (!eqFile.exists()) {
            plugin.saveResource("equipment.yml", false);
        }
        eqConfig = YamlConfiguration.loadConfiguration(eqFile);
    }

    private void loadEntries() {
        entries.clear();
        ConfigurationSection section = eqConfig.getConfigurationSection("equipment");
        
        if (section == null) {
            // Domyślne dane
            entries.add(new EqEntry("latarka", EqEntry.Role.SURVIVOR, EqEntry.Type.ITEM, Material.TORCH, "Latarka", "Oślepia Killera", 1000));
            entries.add(new EqEntry("apteczka", EqEntry.Role.SURVIVOR, EqEntry.Type.ITEM, Material.RED_DYE, "Apteczka", "Leczy rany", 800));
            entries.add(new EqEntry("sprint", EqEntry.Role.SURVIVOR, EqEntry.Type.PERK, Material.FEATHER, "Sprint", "Biegasz szybciej", 2000));
            return;
        }

        for (String key : section.getKeys(false)) {
            String name = section.getString(key + ".name", key);
            String desc = section.getString(key + ".description", "");
            String matName = section.getString(key + ".material", "PAPER");
            int cost = section.getInt(key + ".cost", 0);
            
            String typeStr = section.getString(key + ".type", "ITEM");
            String roleStr = section.getString(key + ".role", "SURVIVOR");

            Material mat = Material.getMaterial(matName.toUpperCase());
            if (mat == null) mat = Material.PAPER;
            
            EqEntry.Type type;
            try { type = EqEntry.Type.valueOf(typeStr.toUpperCase()); } catch (Exception e) { type = EqEntry.Type.ITEM; }
            
            EqEntry.Role role;
            try { role = EqEntry.Role.valueOf(roleStr.toUpperCase()); } catch (Exception e) { role = EqEntry.Role.SURVIVOR; }

            // Tworzymy obiekt Twojej klasy EqEntry
            entries.add(new EqEntry(key, role, type, mat, name, desc, cost));
        }
    }

    public void giveMatchEquipment(Player p, EqEntry.Role role) {
        String itemId = getEquippedItem(p, role);
        if (itemId != null) {
            EqEntry entry = getEntry(itemId);
            if (entry != null && entry.getType() == EqEntry.Type.ITEM) {
                p.getInventory().addItem(new ItemStack(entry.getMaterial()));
            }
        }
    }

    public List<EqEntry> getAvailable(EqEntry.Role role, EqEntry.Type type) {
        List<EqEntry> result = new ArrayList<>();
        for (EqEntry e : entries) {
            if (e.getRole() == role && e.getType() == type) {
                result.add(e);
            }
        }
        return result;
    }

    public boolean hasUnlocked(Player p, String id) {
        return true; 
    }

    public String getEquippedItem(Player p, EqEntry.Role role) {
        return equippedItems.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .get(role);
    }

    public void setEquippedItem(Player p, EqEntry.Role role, String itemId) {
        equippedItems.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(role, itemId);
    }

    public List<String> getEquippedPerks(Player p, EqEntry.Role role) {
        return equippedPerks.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(role, new ArrayList<>());
    }

    public void togglePerk(Player p, EqEntry.Role role, String perkId) {
        List<String> perks = new ArrayList<>(getEquippedPerks(p, role));
        if (perks.contains(perkId)) {
            perks.remove(perkId);
        } else {
            if (perks.size() < 4) {
                perks.add(perkId);
            } else {
                p.sendMessage("§cMax 4 perki!");
            }
        }
        equippedPerks.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>()).put(role, perks);
    }

    public void openEquipment(Player p) {
        try {
            // Jeśli nadal masz EquipmentGUI, to zadziała.
            // Jeśli nie, ten kod się nie wykona (trzeba by dodać prosty fallback)
             new pl.dbd.equipment.EquipmentGUI(plugin, p).open(); 
        } catch (NoClassDefFoundError | Exception e) {
             p.sendMessage("§eGUI nie jest dostępne. (Brak klasy EquipmentGUI)");
        }
    }

    public EqEntry getEntry(String id) {
        for (EqEntry e : entries) if (e.getId().equalsIgnoreCase(id)) return e;
        return null;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
    }
}