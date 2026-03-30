package pl.dbd.economy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class NexusSoulsManager {
   private final DBDPlugin plugin;
   private final File soulsDir;
   private final Map<UUID, Long> cache = new HashMap();

   public NexusSoulsManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.soulsDir = new File(plugin.getDataFolder(), "playerdata/souls");
      if (!this.soulsDir.exists()) {
         this.soulsDir.mkdirs();
      }

   }

   private File getPlayerFile(UUID uuid) {
      return new File(this.soulsDir, uuid.toString() + ".yml");
   }

   private FileConfiguration load(UUID uuid) {
      File file = this.getPlayerFile(uuid);
      if (!file.exists()) {
         try {
            file.createNewFile();
         } catch (IOException var4) {
         }
      }

      return YamlConfiguration.loadConfiguration(file);
   }

   private void save(UUID uuid, FileConfiguration cfg) {
      try {
         cfg.save(this.getPlayerFile(uuid));
      } catch (IOException var4) {
         Logger var10000 = this.plugin.getLogger();
         String var10001 = String.valueOf(uuid);
         var10000.warning("[Souls] Błąd zapisu dla " + var10001 + ": " + var4.getMessage());
      }

   }

   public long getBalance(Player p) {
      return this.getBalance(p.getUniqueId());
   }

   public long getBalance(UUID uuid) {
      if (this.cache.containsKey(uuid)) {
         return (Long)this.cache.get(uuid);
      } else {
         FileConfiguration cfg = this.load(uuid);
         long balance = cfg.getLong("balance", 0L);
         this.cache.put(uuid, balance);
         return balance;
      }
   }

   public void setBalance(Player p, long amount) {
      this.setBalance(p.getUniqueId(), p.getName(), Math.max(0L, amount));
   }

   public void setBalance(UUID uuid, String name, long amount) {
      FileConfiguration cfg = this.load(uuid);
      cfg.set("balance", Math.max(0L, amount));
      cfg.set("name", name);
      cfg.set("last-update", System.currentTimeMillis());
      this.save(uuid, cfg);
      this.cache.put(uuid, Math.max(0L, amount));
   }

   public boolean has(Player p, long amount) {
      return this.getBalance(p) >= amount;
   }

   public boolean add(Player p, long amount) {
      if (amount <= 0L) {
         return false;
      } else {
         this.setBalance(p, this.getBalance(p) + amount);
         return true;
      }
   }

   public boolean take(Player p, long amount) {
      if (amount <= 0L) {
         return false;
      } else {
         long current = this.getBalance(p);
         if (current < amount) {
            return false;
         } else {
            this.setBalance(p, current - amount);
            return true;
         }
      }
   }

   public void reward(Player p, long amount, String reason) {
      if (p != null && p.isOnline()) {
         this.add(p, amount);
         p.sendActionBar("§6+" + amount + " §eDusz Nexusa §8(" + reason + ")");
      }
   }

   public void punish(Player p, long amount, String reason) {
      if (p != null && p.isOnline()) {
         this.take(p, amount);
         p.sendActionBar("§c-" + amount + " §6Dusz Nexusa §8(" + reason + ")");
      }
   }

   public List<Entry<String, Long>> getTopPlayers(int limit) {
      List<Entry<String, Long>> list = new ArrayList();
      File[] files = this.soulsDir.listFiles((dir, namex) -> {
         return namex.endsWith(".yml");
      });
      if (files == null) {
         return list;
      } else {
         File[] var4 = files;
         int var5 = files.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            File file = var4[var6];

            try {
               FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
               String name = cfg.getString("name", file.getName().replace(".yml", ""));
               long balance = cfg.getLong("balance", 0L);
               if (balance > 0L) {
                  list.add(Map.entry(name, balance));
               }
            } catch (Exception var12) {
               this.plugin.getLogger().warning("[Souls] Błąd odczytu " + file.getName());
            }
         }

         list.sort((a, b) -> {
            return Long.compare((Long)b.getValue(), (Long)a.getValue());
         });
         return list.subList(0, Math.min(limit, list.size()));
      }
   }

   public void unloadPlayer(UUID uuid) {
      this.cache.remove(uuid);
   }
}
