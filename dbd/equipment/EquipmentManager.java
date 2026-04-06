package pl.dbd.equipment;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Główny manager ekwipunku - Wersja V2.1 (Naprawione Hex-Gradienty)
 */
public class EquipmentManager {

    private final DBDPlugin plugin;
    private final File eqFile;
    private FileConfiguration data;
    private final Map<String, EqEntry> registry = new LinkedHashMap<>();
    private final EquipmentGUI gui;
    private boolean equipmentBlocked = false;

    public boolean isEquipmentBlocked() {
        return equipmentBlocked;
    }

    public void setEquipmentBlocked(boolean blocked) {
        this.equipmentBlocked = blocked;
    }

    public EquipmentManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.eqFile = new File(plugin.getDataFolder(), "equipment.yml");
        load();
        this.gui = new EquipmentGUI(plugin);
    }

    public EquipmentGUI getGui() {
        return this.gui;
    }

    public Set<String> getAllRegisteredIds() {
        return registry.keySet();
    }

    public void load() {
        if (!eqFile.exists()) {
            try {
                eqFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(eqFile);
        checkDataMigration();
        loadRegistry();
    }

    private void checkDataMigration() {
        if (!data.contains("players"))
            return;
        boolean changed = false;
        for (String uuid : data.getConfigurationSection("players").getKeys(false)) {
            String uPath = "players." + uuid;
            if (data.contains(uPath + ".unlocked")) {
                List<String> unlocked = data.getStringList(uPath + ".unlocked");
                for (String id : unlocked) {
                    data.set(uPath + ".inventory." + id, 1);
                }
                data.set(uPath + ".unlocked", null);
                changed = true;
            }
        }
        if (changed)
            save();
    }

    private void loadRegistry() {
        registry.clear();
        ConfigurationSection sec = data.getConfigurationSection("equipment");
        if (sec == null)
            return;

        for (String key : sec.getKeys(false)) {
            String roleStr = sec.getString(key + ".role", "SURVIVOR");
            String typeStr = sec.getString(key + ".type", "ITEM");
            String matStr = sec.getString(key + ".material", "PAPER");
            String name = color(sec.getString(key + ".name", key));

            List<String> rawDesc = sec.getStringList(key + ".description");
            List<String> desc = new ArrayList<>();
            for (String line : rawDesc)
                desc.add(color(line));

            EqEntry.Role role = EqEntry.Role.SURVIVOR;
            try {
                role = EqEntry.Role.valueOf(roleStr.toUpperCase());
            } catch (Exception ignored) {
            }

            EqEntry.Type type = EqEntry.Type.ITEM;
            try {
                type = EqEntry.Type.valueOf(typeStr.toUpperCase());
            } catch (Exception ignored) {
            }

            Material mat = Material.matchMaterial(matStr.toUpperCase());
            if (mat == null)
                mat = Material.PAPER;

            if (name == null)
                name = mat.name();

            if (desc == null)
                desc = new ArrayList<>();

            boolean consumable = sec.getBoolean(key + ".consumable", false);

            registry.put(key, new EqEntry(type, role, key, name, mat, desc, consumable));
        }
        plugin.getLogger().info("[EQ] Rejestr zaktualizowany. Gradienty załadowane.");
    }

    public void save() {
        try {
            data.save(eqFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[EQ] Błąd zapisu pliku YAML: " + e.getMessage());
        }
    }

    // ── SYSTEM ODBLOKOWYWANIA ORAZ ILOŚCI ──

    public boolean unlockPerk(Player p, String id) {
        return addEqAmount(p, id, 1);
    }

    public boolean addEqAmount(Player p, String id, int amount) {
        if (!registry.containsKey(id))
            return false;

        int cur = data.getInt(uuid(p) + ".inventory." + id, 0);
        data.set(uuid(p) + ".inventory." + id, cur + amount);
        save();
        return true;
    }

    public boolean lockPerk(Player p, String id) {
        if (!registry.containsKey(id))
            return false;

        int cur = data.getInt(uuid(p) + ".inventory." + id, 0);
        if (cur <= 0)
            return false;

        data.set(uuid(p) + ".inventory." + id, null);

        // Usuń z equipped jeśli był założony
        for (EqEntry.Role role : EqEntry.Role.values()) {
            String equippedItem = getEquippedItem(p, role);
            if (id.equals(equippedItem)) {
                data.set(uuid(p) + ".equipped." + role.name() + ".item", null);
            }
            List<String> equippedPerks = new ArrayList<>(getEquippedPerks(p, role));
            if (equippedPerks.remove(id)) {
                data.set(uuid(p) + ".equipped." + role.name() + ".perks", equippedPerks);
            }
        }

        save();
        return true;
    }

    public List<String> getUnlockedPerkIds(Player p) {
        ConfigurationSection inv = data.getConfigurationSection(uuid(p) + ".inventory");
        if (inv == null)
            return new ArrayList<>();
        return new ArrayList<>(inv.getKeys(false));
    }

    public int getEqAmount(Player p, String id) {
        return data.getInt(uuid(p) + ".inventory." + id, 0);
    }

    // ── METODY DLA GUI ──

    public List<EqEntry> getOwnedItems(Player p, EqEntry.Role role) {
        List<EqEntry> out = new ArrayList<>();
        ConfigurationSection inv = data.getConfigurationSection(uuid(p) + ".inventory");
        if (inv != null) {
            for (String id : inv.getKeys(false)) {
                if (inv.getInt(id) > 0) {
                    EqEntry e = registry.get(id);
                    if (e != null && e.role() == role && e.type() == EqEntry.Type.ITEM)
                        out.add(e);
                }
            }
        }
        return out;
    }

    public List<EqEntry> getUnlockedPerks(Player p, EqEntry.Role role) {
        List<EqEntry> out = new ArrayList<>();
        ConfigurationSection inv = data.getConfigurationSection(uuid(p) + ".inventory");
        if (inv != null) {
            for (String id : inv.getKeys(false)) {
                if (inv.getInt(id) > 0) {
                    EqEntry e = registry.get(id);
                    if (e != null && e.role() == role && e.type() == EqEntry.Type.PERK)
                        out.add(e);
                }
            }
        }
        return out;
    }

    public String getEquippedItem(Player p, EqEntry.Role role) {
        return data.getString(uuid(p) + ".equipped." + role.name() + ".item", null);
    }

    public void toggleEquippedItem(Player p, EqEntry.Role role, String id) {
        String current = getEquippedItem(p, role);
        if (id.equals(current)) {
            data.set(uuid(p) + ".equipped." + role.name() + ".item", null);
        } else {
            data.set(uuid(p) + ".equipped." + role.name() + ".item", id);
        }
        save();
    }

    public List<String> getEquippedPerks(Player p, EqEntry.Role role) {
        return data.getStringList(uuid(p) + ".equipped." + role.name() + ".perks");
    }

    public void toggleEquippedPerk(Player p, EqEntry.Role role, String id) {
        List<String> equipped = getEquippedPerks(p, role);
        if (equipped.contains(id)) {
            equipped.remove(id);
        } else {
            if (equipped.size() >= 4) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cosignitolimit4perkw"));
                return;
            }
            equipped.add(id);
        }
        data.set(uuid(p) + ".equipped." + role.name() + ".perks", equipped);
        save();
    }

    // ── ADAPTERY I KLUCZE ──

    public void giveMatchEquipment(Player p, EqEntry.Role role) {
        if (equipmentBlocked) {
            p.sendMessage(
                    "§cOpcja używania perków i przedmiotów w tej rozgrywce została zablokowana przez administrację!");
            return;
        }

        String itemId = getEquippedItem(p, role);
        if (itemId != null && registry.containsKey(itemId)) {
            EqEntry itemEntry = registry.get(itemId);
            p.getInventory().addItem(createEqItemStack(itemEntry));
        }

        List<String> perks = getEquippedPerks(p, role);
        for (String perkId : perks) {
            if (registry.containsKey(perkId)) {
                EqEntry perkEntry = registry.get(perkId);
                p.getInventory().addItem(createEqItemStack(perkEntry));
            }
        }
    }

    private ItemStack createEqItemStack(EqEntry entry) {
        ItemStack is = new ItemStack(entry.material());
        org.bukkit.inventory.meta.ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(entry.display());
            if (entry.desc() != null && !entry.desc().isEmpty()) {
                meta.setLore(entry.desc());
            }
            is.setItemMeta(meta);
        }
        return is;
    }

    public EqEntry getEntry(String id) {
        return registry.get(id);
    }

    public void addGuiEntryFromAdmin(String id, ItemStack item) {
        String path = "equipment." + id;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        data.set(path + ".role", "SURVIVOR");
        data.set(path + ".type", "ITEM");
        data.set(path + ".material", item.getType().name());

        if (meta != null) {
            if (meta.hasDisplayName()) {
                String safeName = meta.getDisplayName().replace("§", "&");
                data.set(path + ".name", safeName);
            } else {
                data.set(path + ".name", item.getType().name());
            }

            if (meta.hasLore()) {
                List<String> lore = new ArrayList<>();
                for (String l : meta.getLore()) {
                    lore.add(l.replace("§", "&"));
                }
                data.set(path + ".description", lore);
            }
        }

        save();
        loadRegistry(); // przeładuj
    }

    public void removeGuiEntry(String id) {
        data.set("equipment." + id, null);
        save();
        loadRegistry();
    }

    public void toggleGuiRole(String id) {
        String path = "equipment." + id + ".role";
        String cur = data.getString(path, "SURVIVOR");
        data.set(path, cur.equalsIgnoreCase("SURVIVOR") ? "KILLER" : "SURVIVOR");
        save();
        loadRegistry();
    }

    public void toggleGuiType(String id) {
        String path = "equipment." + id + ".type";
        String cur = data.getString(path, "ITEM");
        data.set(path, cur.equalsIgnoreCase("ITEM") ? "PERK" : "ITEM");
        save();
        loadRegistry();
    }

    public void toggleGuiConsumable(String id) {
        String path = "equipment." + id + ".consumable";
        boolean cur = data.getBoolean(path, false);
        data.set(path, !cur);
        save();
        loadRegistry();
    }

    // ── SYSTEM KOLORÓW HEX ──

    private String uuid(Player p) {
        return "players." + p.getUniqueId();
    }

    private void touch(Player p) {
        data.set(uuid(p) + ".name", p.getName());
    }

    /**
     * Konwertuje tagi <##RRGGBB> na format §x§r§r§g§g§b§b oraz obsługuje
     * standardowe '&'.
     */
    public static String color(String text) {
        if (text == null || text.isEmpty())
            return "";

        // Przetwarzanie HEX: <##24FD2A> -> §x§2§4§F§D§2§A
        Pattern pattern = Pattern.compile("<##([A-Fa-f0-9]{6})>");
        Matcher matcher = pattern.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            // Matcher.quoteReplacement zabezpiecza przed błędami przy znakach specjalnych
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);

        // Na koniec standardowe & -> §
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}