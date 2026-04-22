package pl.dbd.locker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class LockerManager {
   private final DBDPlugin plugin;
   private final List<Locker> lockers;
   private final File dataFile;

   public LockerManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.lockers = new ArrayList();
      this.dataFile = new File(plugin.getDataFolder(), "lockers.yml");
      this.loadLockers();
   }

   public void createLocker(Location doorLocation) {
      Block doorBlock = doorLocation.getBlock();
      if (this.isDoor(doorBlock.getType())) {
         BlockFace facing = BlockFace.NORTH;
         if (doorBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional)doorBlock.getBlockData();
            facing = directional.getFacing();
         }

         Locker locker = new Locker(doorLocation, facing);
         this.lockers.add(locker);
         this.saveLockers();
         Logger var10000 = this.plugin.getLogger();
         int var10001 = doorLocation.getBlockX();
         var10000.info("Utworzono szafę na: " + var10001 + ", " + doorLocation.getBlockY() + ", " + doorLocation.getBlockZ() + " (kierunek: " + String.valueOf(facing) + ")");
      }

   }

   public void removeLocker(Location location) {
      this.lockers.removeIf((locker) -> {
         if (locker.getDoorLocation().getBlock().equals(location.getBlock())) {
            if (locker.hasPlayerInside()) {
               Player player = Bukkit.getPlayer(locker.getPlayerInside());
               if (player != null) {
                  locker.exitLocker(player, this.plugin);
               }
            }

            return true;
         } else {
            return false;
         }
      });
      this.saveLockers();
   }

   public Locker getLockerAt(Location location) {
      Iterator var2 = this.lockers.iterator();

      while(var2.hasNext()) {
         Locker locker = (Locker)var2.next();
         if (locker.getDoorLocation().getBlock().equals(location.getBlock())) {
            return locker;
         }
      }

      return null;
   }

   public Locker getLockerForPlayer(UUID playerUUID) {
      Iterator var2 = this.lockers.iterator();

      Locker locker;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         locker = (Locker)var2.next();
      } while(!locker.hasPlayerInside() || !locker.getPlayerInside().equals(playerUUID));

      return locker;
   }

   public List<Locker> getAllLockers() {
      return new ArrayList(this.lockers);
   }

   public void resetAllLockers() {
      Iterator var1 = this.lockers.iterator();

      while(var1.hasNext()) {
         Locker locker = (Locker)var1.next();
         if (locker.hasPlayerInside()) {
            Player player = Bukkit.getPlayer(locker.getPlayerInside());
            if (player != null) {
               locker.exitLocker(player, this.plugin);
            }
         }
      }

   }

   private boolean isDoor(Material material) {
      return material == Material.OAK_DOOR || material == Material.SPRUCE_DOOR || material == Material.BIRCH_DOOR || material == Material.JUNGLE_DOOR || material == Material.ACACIA_DOOR || material == Material.DARK_OAK_DOOR || material == Material.CRIMSON_DOOR || material == Material.WARPED_DOOR || material == Material.MANGROVE_DOOR || material == Material.CHERRY_DOOR || material == Material.BAMBOO_DOOR || material == Material.IRON_DOOR;
   }

   public void saveLockers() {
      try {
         FileConfiguration config = new YamlConfiguration();
         List<Map<String, Object>> lockerList = (List)this.lockers.stream().map(Locker::serialize).collect(Collectors.toList());
         config.set("lockers", lockerList);
         config.save(this.dataFile);
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Nie można zapisać szaf: " + var3.getMessage());
      }

   }

   private void loadLockers() {
      if (this.dataFile.exists()) {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(this.dataFile);
            List<Map<?, ?>> lockerList = config.getMapList("lockers");
            Iterator var3 = lockerList.iterator();

            while(var3.hasNext()) {
               Map<?, ?> data = (Map)var3.next();
               String worldName = (String)data.get("world");
               double x = ((Number)data.get("x")).doubleValue();
               double y = ((Number)data.get("y")).doubleValue();
               double z = ((Number)data.get("z")).doubleValue();
               String facingStr = (String)data.get("facing");
               Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
               BlockFace facing = BlockFace.valueOf(facingStr);
               Locker locker = new Locker(loc, facing);
               this.lockers.add(locker);
            }

            this.plugin.getLogger().info("Wczytano " + this.lockers.size() + " szaf");
         } catch (Exception var16) {
            this.plugin.getLogger().warning("Nie można wczytać szaf: " + var16.getMessage());
            var16.printStackTrace();
         }
      }

   }
}
