package pl.dbd.window;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.dbd.DBDPlugin;

public class WindowManager {
   private final File file;
   private final YamlConfiguration config;
   private final Set<Location> windows = new HashSet();

   public WindowManager(DBDPlugin plugin) {
      this.file = new File(plugin.getDataFolder(), "windows.yml");
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

      this.config = YamlConfiguration.loadConfiguration(this.file);
      this.load();
   }

   private void load() {
      this.windows.clear();
      if (this.config.isConfigurationSection("windows")) {
         Iterator var1 = this.config.getConfigurationSection("windows").getKeys(false).iterator();

         while(var1.hasNext()) {
            String k = (String)var1.next();
            Location loc = this.config.getLocation("windows." + k);
            if (loc != null) {
               this.windows.add(loc);
            }
         }
      }

   }

   private void save() {
      this.config.set("windows", (Object)null);
      int i = 0;
      Iterator var2 = this.windows.iterator();

      while(var2.hasNext()) {
         Location l = (Location)var2.next();
         int var10001 = i++;
         this.config.set("windows." + var10001, l);
      }

      try {
         this.config.save(this.file);
      } catch (IOException var5) {
         Bukkit.getLogger().severe("Nie mozna zapisac windows.yml");
      }

   }

   public void toggle(Location loc) {
      if (this.windows.contains(loc)) {
         this.windows.remove(loc);
      } else {
         this.windows.add(loc);
      }

      this.save();
   }

   public boolean isWindow(Location loc) {
      return this.windows.contains(loc);
   }

   public int getWindowCount() {
      return this.windows.size();
   }

   public Set<Location> getAllWindows() {
      return new HashSet(this.windows);
   }

   public void resetAllWindows() {
      this.windows.clear();
      this.save();
   }
}
