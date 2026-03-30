package pl.dbd.locker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LockerManager {
   private final DBDPlugin plugin;
   // Per-mapa storage — surowe stringi lokalizacji (world,x,y,z), aby uniknąć
   // problemów z niezaładowanymi worldami
   private final Map<String, List<String>> mapLockerStrings = new HashMap<>();
   // Aktywne szafy (aktualnie załadowane na daną mapę)
   private final List<Locker> activeLockers = new ArrayList<>();
   private String currentMapName = null;
   private final File dataFile;

   public LockerManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.dataFile = new File(plugin.getDataFolder(), "lockers.yml");
      load();
   }

   // ── LOAD / SAVE ──

   public void load() {
      if (!dataFile.exists()) {
         try {
            dataFile.createNewFile();
         } catch (IOException ignored) {
         }
      }
      FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
      mapLockerStrings.clear();
      activeLockers.clear();

      if (data.contains("lockers")) {
         if (data.isConfigurationSection("lockers")) {
            for (String mapName : data.getConfigurationSection("lockers").getKeys(false)) {
               List<String> list = new ArrayList<>();
               if (data.isConfigurationSection("lockers." + mapName)) {
                  for (String key : data.getConfigurationSection("lockers." + mapName).getKeys(false)) {
                     String locStr = data.getString("lockers." + mapName + "." + key);
                     if (locStr != null && !locStr.isEmpty()) {
                        list.add(locStr);
                     }
                  }
               }
               if (!list.isEmpty()) {
                  mapLockerStrings.put(mapName, list);
               }
            }
         }
      }

      int total = 0;
      for (List<String> list : mapLockerStrings.values()) {
         total += list.size();
      }
      plugin.getLogger().info("[Lockers] Zaladowano " + total + " szaf z " + mapLockerStrings.size() + " map");
   }

   public void save() {
      FileConfiguration data = new YamlConfiguration();

      // Najpierw zapisz aktualnie aktywne szafy z powrotem do raw strings
      if (currentMapName != null && !activeLockers.isEmpty()) {
         List<String> current = new ArrayList<>();
         for (Locker l : activeLockers) {
            Location loc = l.getDoorLocation();
            if (loc.getWorld() != null) {
               current.add(
                     loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
         }
         mapLockerStrings.put(currentMapName, current);
      }

      for (Map.Entry<String, List<String>> entry : mapLockerStrings.entrySet()) {
         String mapName = entry.getKey();
         List<String> list = entry.getValue();
         for (int i = 0; i < list.size(); i++) {
            data.set("lockers." + mapName + "." + i, list.get(i));
         }
      }

      try {
         data.save(dataFile);
      } catch (IOException e) {
         plugin.getLogger().warning("[Lockers] Nie mozna zapisac szaf: " + e.getMessage());
      }
   }

   private Location parseLocation(String str) {
      if (str == null || str.isEmpty())
         return null;
      String[] parts = str.split(",");
      if (parts.length != 4)
         return null;
      try {
         org.bukkit.World world = Bukkit.getWorld(parts[0]);
         if (world == null) {
            plugin.getLogger().warning("[Lockers] Swiat '" + parts[0] + "' nie jest zaladowany!");
            return null;
         }
         return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
               Double.parseDouble(parts[3]));
      } catch (Exception e) {
         return null;
      }
   }

   // ── PER-MAP AKTYWACJA ──

   public void activateMap(String mapName) {
      // Wyczyść aktywne szafy
      for (Locker l : activeLockers) {
         if (l.hasPlayerInside()) {
            Player p = Bukkit.getPlayer(l.getPlayerInside());
            if (p != null)
               l.exitLocker(p, plugin);
         }
      }
      activeLockers.clear();
      currentMapName = mapName;

      // Utwórz obiekty Locker z surowych stringów (teraz worldy powinny być
      // załadowane)
      List<String> strings = mapLockerStrings.get(mapName);
      if (strings != null) {
         for (String locStr : strings) {
            Location loc = parseLocation(locStr);
            if (loc != null) {
               activeLockers.add(new Locker(loc));
            }
         }
         plugin.getLogger().info("[Lockers] Aktywowano " + activeLockers.size() + " szaf dla mapy: " + mapName);
      } else {
         plugin.getLogger().warning("[Lockers] Brak szaf dla mapy: " + mapName);
      }
   }

   public void deactivateMap() {
      resetAllLockers();
      // Zapisz bieżący stan aktywnych lockerów z powrotem do stringów
      if (currentMapName != null && !activeLockers.isEmpty()) {
         List<String> current = new ArrayList<>();
         for (Locker l : activeLockers) {
            Location loc = l.getDoorLocation();
            if (loc.getWorld() != null) {
               current.add(
                     loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
         }
         mapLockerStrings.put(currentMapName, current);
      }
      activeLockers.clear();
      currentMapName = null;
   }

   // ── TWORZENIE / USUWANIE ──

   public void createLocker(Location doorLoc, String mapName) {
      // Normalizuj do dolnej części drzwi
      Block block = doorLoc.getBlock();
      if (block.getBlockData() instanceof Door) {
         Door door = (Door) block.getBlockData();
         if (door.getHalf() == Door.Half.TOP) {
            doorLoc = block.getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
         }
      }

      // Dodaj do raw strings
      String locStr = doorLoc.getWorld().getName() + "," + doorLoc.getBlockX() + "," + doorLoc.getBlockY() + ","
            + doorLoc.getBlockZ();
      mapLockerStrings.computeIfAbsent(mapName, k -> new ArrayList<>()).add(locStr);

      // Jeśli ta mapa jest aktywna, dodaj do aktywnych jako obiekt Locker
      if (mapName.equalsIgnoreCase(currentMapName)) {
         activeLockers.add(new Locker(doorLoc));
      }

      save();
   }

   public boolean removeLocker(Location loc) {
      // Normalizuj do dolnej części drzwi
      Block block = loc.getBlock();
      if (block.getBlockData() instanceof Door) {
         Door door = (Door) block.getBlockData();
         if (door.getHalf() == Door.Half.TOP) {
            loc = block.getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
         }
      }

      String targetStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
            + loc.getBlockZ();

      boolean removed = false;
      for (List<String> list : mapLockerStrings.values()) {
         if (list.remove(targetStr)) {
            removed = true;
            break;
         }
      }

      if (removed) {
         Location finalLoc = loc;
         activeLockers.removeIf(l -> l.getDoorLocation().getBlock().equals(finalLoc.getBlock()));
         save();
      }
      return removed;
   }

   // ── WYSZUKIWANIE ──

   public Locker getLockerAt(Location loc) {
      // Normalizuj do dolnej części drzwi
      Block block = loc.getBlock();
      if (block.getBlockData() instanceof Door) {
         Door door = (Door) block.getBlockData();
         if (door.getHalf() == Door.Half.TOP) {
            loc = block.getRelative(org.bukkit.block.BlockFace.DOWN).getLocation();
         }
      }

      for (Locker l : activeLockers) {
         if (l.getDoorLocation().getBlock().equals(loc.getBlock())) {
            return l;
         }
      }
      return null;
   }

   public Locker getLockerForPlayer(UUID playerUUID) {
      for (Locker l : activeLockers) {
         if (l.hasPlayerInside() && l.getPlayerInside().equals(playerUUID)) {
            return l;
         }
      }
      return null;
   }

   public List<Locker> getAllLockers() {
      return new ArrayList<>(activeLockers);
   }

   public Set<String> getMapNames() {
      return mapLockerStrings.keySet();
   }

   public void resetAllLockers() {
      for (Locker l : activeLockers) {
         if (l.hasPlayerInside()) {
            Player p = Bukkit.getPlayer(l.getPlayerInside());
            if (p != null) {
               l.exitLocker(p, plugin);
            }
         }
      }
   }
}
