package pl.dbd.currency;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class BloodPointsManager {
   private final DBDPlugin plugin;
   private final Map<UUID, BloodPointsManager.PlayerData> playerData;
   private final File dataFile;

   public BloodPointsManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.playerData = new HashMap();
      this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
      this.loadData();
   }

   public void addBloodPoints(Player player, int amount, String reason) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      data.bloodPoints += amount;
      player.sendMessage("§c§l+ " + amount + " kropli krwi nexusa §7(" + reason + ")");
      player.sendActionBar("§eSaldo: §c" + data.bloodPoints + " kropli");
      this.saveData();
   }

   public void removeBloodPoints(Player player, int amount) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      data.bloodPoints = Math.max(0, data.bloodPoints - amount);
      this.saveData();
   }

   public void setBloodPoints(Player player, int amount) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      data.bloodPoints = amount;
      this.saveData();
   }

   public int getBloodPoints(Player player) {
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.get(player.getUniqueId());
      return data != null ? data.bloodPoints : 0;
   }

   public boolean hasEnough(Player player, int amount) {
      return this.getBloodPoints(player) >= amount;
   }

   public void addWin(Player player) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      ++data.wins;
      this.saveData();
   }

   public void addLoss(Player player) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      ++data.losses;
      this.saveData();
   }

   public void addKill(Player player) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      ++data.kills;
      this.saveData();
   }

   public void addEscape(Player player) {
      UUID uuid = player.getUniqueId();
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.computeIfAbsent(uuid,
            (k) -> {
               return new BloodPointsManager.PlayerData();
            });
      ++data.escapes;
      this.saveData();
   }

   public int getWins(Player player) {
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.get(player.getUniqueId());
      return data != null ? data.wins : 0;
   }

   public int getLosses(Player player) {
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.get(player.getUniqueId());
      return data != null ? data.losses : 0;
   }

   public int getKills(Player player) {
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.get(player.getUniqueId());
      return data != null ? data.kills : 0;
   }

   public int getEscapes(Player player) {
      BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) this.playerData.get(player.getUniqueId());
      return data != null ? data.escapes : 0;
   }

   public int getGamesPlayed(Player player) {
      return this.getWins(player) + this.getLosses(player);
   }

   public void grantSouls(Player player, int amount, String reason) {
      if (player != null && reason != null) {
         if (amount <= 0) {
            amount = 1;
         }

         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusz dodaj {player} {amount} {reason}");
         cmd = cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}",
               reason);
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
      }
   }

   public void saveData() {
      try {
         FileConfiguration config = new YamlConfiguration();
         Iterator var2 = this.playerData.entrySet().iterator();

         while (var2.hasNext()) {
            Entry<UUID, BloodPointsManager.PlayerData> entry = (Entry) var2.next();
            String uuid = ((UUID) entry.getKey()).toString();
            BloodPointsManager.PlayerData data = (BloodPointsManager.PlayerData) entry.getValue();
            config.set(uuid + ".bloodpoints", data.bloodPoints);
            config.set(uuid + ".wins", data.wins);
            config.set(uuid + ".losses", data.losses);
            config.set(uuid + ".kills", data.kills);
            config.set(uuid + ".escapes", data.escapes);
         }

         config.save(this.dataFile);
      } catch (IOException var6) {
         this.plugin.getLogger().warning("Nie można zapisać danych graczy: " + var6.getMessage());
      }

   }

   private void loadData() {
      if (this.dataFile.exists()) {
         try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(this.dataFile);
            Iterator var2 = config.getKeys(false).iterator();

            while (var2.hasNext()) {
               String key = (String) var2.next();

               try {
                  UUID uuid = UUID.fromString(key);
                  BloodPointsManager.PlayerData data = new BloodPointsManager.PlayerData();
                  data.bloodPoints = config.getInt(key + ".bloodpoints", 0);
                  data.wins = config.getInt(key + ".wins", 0);
                  data.losses = config.getInt(key + ".losses", 0);
                  data.kills = config.getInt(key + ".kills", 0);
                  data.escapes = config.getInt(key + ".escapes", 0);
                  this.playerData.put(uuid, data);
               } catch (IllegalArgumentException var6) {
                  this.plugin.getLogger().warning("Nieprawidłowy UUID: " + key);
               }
            }

            this.plugin.getLogger().info("Wczytano " + this.playerData.size() + " profili graczy");
         } catch (Exception var7) {
            this.plugin.getLogger().warning("Nie można wczytać danych graczy: " + var7.getMessage());
         }
      }

   }

   public void clearPlayer(Player player) {
      this.playerData.remove(player.getUniqueId());
      this.saveData();
   }

   public Map<UUID, Integer> getTopPlayers(int limit) {
      return this.playerData.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().bloodPoints, e1.getValue().bloodPoints))
            .limit(limit)
            .collect(java.util.LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().bloodPoints),
                  java.util.LinkedHashMap::putAll);
   }

   public Map<UUID, Integer> getTopPlayersByGames(int limit) {
      return this.playerData.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().wins + e2.getValue().losses,
                  e1.getValue().wins + e1.getValue().losses))
            .limit(limit)
            .collect(java.util.LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().wins + e.getValue().losses),
                  java.util.LinkedHashMap::putAll);
   }

   public Map<UUID, Integer> getTopPlayersByWins(int limit) {
      return this.playerData.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().wins, e1.getValue().wins))
            .limit(limit)
            .collect(java.util.LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().wins),
                  java.util.LinkedHashMap::putAll);
   }

   public Map<UUID, Integer> getTopPlayersByLosses(int limit) {
      return this.playerData.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().losses, e1.getValue().losses))
            .limit(limit)
            .collect(java.util.LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().losses),
                  java.util.LinkedHashMap::putAll);
   }

   private static class PlayerData {
      int bloodPoints = 0;
      int wins = 0;
      int losses = 0;
      int kills = 0;
      int escapes = 0;
   }
}
