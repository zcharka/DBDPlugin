package pl.dbd.stats;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class PlayerStatsManager {
   private final DBDPlugin plugin;
   private final File statsFile;
   private FileConfiguration stats;

   public PlayerStatsManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
      this.load();
   }

   private void load() {
      if (!this.statsFile.exists()) {
         try {
            this.statsFile.createNewFile();
         } catch (IOException var2) {
         }
      }

      this.stats = YamlConfiguration.loadConfiguration(this.statsFile);
   }

   private void save() {
      try {
         this.stats.save(this.statsFile);
      } catch (IOException var2) {
         this.plugin.getLogger().warning("[Stats] Błąd zapisu stats.yml: " + var2.getMessage());
      }

   }

   public int getWins(Player p) {
      return this.stats.getInt(this.key(p, "wins"), 0);
   }

   public int getLosses(Player p) {
      return this.stats.getInt(this.key(p, "losses"), 0);
   }

   public int getGames(Player p) {
      return this.stats.getInt(this.key(p, "games"), 0);
   }

   public void addWin(Player p) {
      String uuid = p.getUniqueId().toString();
      this.stats.set(uuid + ".name", p.getName());
      this.stats.set(uuid + ".wins", this.getWins(p) + 1);
      this.stats.set(uuid + ".games", this.getGames(p) + 1);
      this.save();
   }

   public void addLoss(Player p) {
      String uuid = p.getUniqueId().toString();
      this.stats.set(uuid + ".name", p.getName());
      this.stats.set(uuid + ".losses", this.getLosses(p) + 1);
      this.stats.set(uuid + ".games", this.getGames(p) + 1);
      this.save();
   }

   private String key(Player p, String field) {
      String var10000 = p.getUniqueId().toString();
      return var10000 + "." + field;
   }
}
