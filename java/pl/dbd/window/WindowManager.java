package pl.dbd.window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.dbd.DBDPlugin;

public class WindowManager {
   private final File file;
   private final YamlConfiguration config;
   private final List<Location> windows = new ArrayList<>();

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

   public void load() {
      this.windows.clear();
      if (this.config.isConfigurationSection("windows")) {
         for (String k : this.config.getConfigurationSection("windows").getKeys(false)) {
            Location loc = this.config.getLocation("windows." + k);
            if (loc != null) {
               this.windows.add(loc);
            }
         }
      }

   }

   private void save() {
      this.config.set("windows", null);
      for (int i = 0; i < this.windows.size(); i++) {
         this.config.set("windows." + i, this.windows.get(i));
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

   public boolean removeWindow(int index) {
      if (index >= 0 && index < this.windows.size()) {
         this.windows.remove(index);
         this.save();
         return true;
      }
      return false;
   }

   public boolean isWindow(Location loc) {
      return this.windows.contains(loc);
   }

   public int getWindowCount() {
      return this.windows.size();
   }

   public List<Location> getAllWindows() {
      return new ArrayList<>(this.windows);
   }

   public void resetAllWindows() {
      this.windows.clear();
      this.save();
   }
}
