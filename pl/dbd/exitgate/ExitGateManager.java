package pl.dbd.exitgate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ExitGateManager {

   private final DBDPlugin plugin;
   private final File gatesFile;
   private FileConfiguration data;

   // Mapa: NazwaMapy -> Lista bram wyjściowych
   private Map<String, List<ExitGate>> mapGates = new HashMap<>();

   // Lista aktywnych bram wyjściowych (na obecnej mapie)
   private List<ExitGate> activeGates = new ArrayList<>();

   private String currentMapName = null;

   /** Endgame Collapse: 2 minuty na opuszczenie mapy po otwarciu bramy. */
   private static final int COLLAPSE_SECONDS = 120;
   private BossBar collapseBossBar = null;
   private BukkitRunnable collapseTask = null;

   public ExitGateManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.gatesFile = new File(plugin.getDataFolder(), "exitgates.yml");
      load();
   }

   public void load() {
      if (!gatesFile.exists()) {
         try {
            gatesFile.createNewFile();
         } catch (IOException ignored) {
         }
      }
      data = YamlConfiguration.loadConfiguration(gatesFile);

      mapGates.clear();
      activeGates.clear();
      currentMapName = null;

      if (data.contains("maps")) {
         ConfigurationSection mapsSection = data.getConfigurationSection("maps");
         for (String mapName : mapsSection.getKeys(false)) {
            List<ExitGate> gates = new ArrayList<>();
            ConfigurationSection gateSection = mapsSection.getConfigurationSection(mapName + ".gates");

            if (gateSection != null) {
               for (String key : gateSection.getKeys(false)) {
                  String locStr = gateSection.getString(key + ".location");
                  Location loc = parseLocation(locStr);
                  if (loc != null) {
                     gates.add(new ExitGate(loc));
                  }
               }
            }
            mapGates.put(mapName, gates);
         }
      }
   }

   public void saveGates() {
      data.set("maps", null); // Czyścimy przed zapisem

      for (Map.Entry<String, List<ExitGate>> entry : mapGates.entrySet()) {
         String mapName = entry.getKey();
         List<ExitGate> gates = entry.getValue();

         for (int i = 0; i < gates.size(); i++) {
            ExitGate gate = gates.get(i);
            Location loc = gate.getLocation();
            String path = "maps." + mapName + ".gates." + i + ".location";
            data.set(path,
                  loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
         }
      }
      try {
         data.save(gatesFile);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void activateMap(String mapName) {
      activeGates.clear();
      currentMapName = mapName;

      if (mapGates.containsKey(mapName)) {
         List<ExitGate> gates = mapGates.get(mapName);
         for (ExitGate gate : gates) {
            gate.reset();
            activeGates.add(gate);
         }
         plugin.getLogger().info("Aktywowano bramy wyjściowe dla mapy: " + mapName);
      }
   }

   public void deactivateMap() {
      stopEndgameCollapse();
      activeGates.clear();
      currentMapName = null;
   }

   /**
    * Uruchamia Endgame Collapse: pomarańczowy boss bar i 2 minuty na ucieczkę.
    * Po upływie czasu gracze pozostali na mapie (survivorzy) umierają.
    * Wywołać przy pierwszym otwarciu bramy.
    */
   private void forceEndgameDeath(Player p) {
      if (p != null) {
         plugin.getGameManager().markDead(p);
         plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.DEAD);
         if (plugin.getHookManager() != null) {
            plugin.getHookManager().unhookPlayer(p);
         }

         p.setHealth(20.0);
         p.setFoodLevel(20);
         p.setWalkSpeed(0.2F);
         for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
         }
         p.setGlowing(false);
         if (plugin.getGameManager() != null) {
            plugin.getGameManager().setPlayerRedGlow(p, false);
         }
         p.getInventory().clear();

         if (plugin.getGameManager().getKilledSpawn() != null) {
            p.teleport(plugin.getGameManager().getKilledSpawn());
         } else if (plugin.getGameManager().getLobbySpawn() != null) {
            p.teleport(plugin.getGameManager().getLobbySpawn());
         }

         p.setGameMode(org.bukkit.GameMode.SPECTATOR);

         String msg = plugin.getConfig().getString("endgame-collapse.death-message",
               "§cNie zdążyłeś uciec przed zapadnięciem się świata!");
         String title = plugin.getConfig().getString("endgame-collapse.death-title", "§c§lKONIEC CZASU");
         String subtitle = plugin.getConfig().getString("endgame-collapse.death-subtitle", "§7Nie zdążyłeś uciec...");

         p.sendMessage(msg);
         p.sendTitle(title, subtitle, 10, 60, 20);

         try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
               org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                     "lp user " + p.getName() + " permission set dbd.oglada true");
            }
         } catch (Exception ex) {
         }
      }
   }

   public void startEndgameCollapse() {
      if (collapseTask != null)
         return;

      int mins = COLLAPSE_SECONDS / 60;
      int secs = COLLAPSE_SECONDS % 60;
      String timeStr = String.format("%02d:%02d", mins, secs);

      collapseBossBar = Bukkit.createBossBar(
            "§6§lEndgame Collapse §7- §f" + timeStr,
            BarColor.YELLOW,
            BarStyle.SOLID);
      collapseBossBar.setProgress(1.0);
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (plugin.getGameManager().isInGame(p))
            collapseBossBar.addPlayer(p);
      }

      final int[] secondsLeft = new int[] { COLLAPSE_SECONDS };
      collapseTask = new BukkitRunnable() {
         @Override
         public void run() {
            if (plugin.getGameManager().getGameState() != GameManager.GameState.IN_GAME) {
               stopEndgameCollapse();
               cancel();
               return;
            }
            secondsLeft[0]--;
            if (secondsLeft[0] <= 0) {
               for (UUID uuid : plugin.getGameManager().getSurvivors()) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && p.isOnline()
                        && !plugin.getGameManager().hasEscaped(p)
                        && !plugin.getGameManager().isDead(p)) {
                     forceEndgameDeath(p);
                  }
               }
               Bukkit.broadcastMessage("§c§lEndgame Collapse! §7Wszyscy pozostali na mapie zginęli.");
               stopEndgameCollapse();
               cancel();
               return;
            }
            double progress = (double) secondsLeft[0] / COLLAPSE_SECONDS;
            int m = secondsLeft[0] / 60;
            int s = secondsLeft[0] % 60;
            String ts = String.format("%02d:%02d", m, s);
            collapseBossBar.setProgress(progress);
            collapseBossBar.setTitle("§6§lEndgame Collapse §7- §f" + ts);
         }
      };
      collapseTask.runTaskTimer(plugin, 20L, 20L);
   }

   /** Zatrzymuje Endgame Collapse i usuwa boss bar. */
   public void stopEndgameCollapse() {
      if (collapseTask != null) {
         collapseTask.cancel();
         collapseTask = null;
      }
      if (collapseBossBar != null) {
         collapseBossBar.removeAll();
         collapseBossBar = null;
      }
   }

   public void resetAllGates() {
      for (ExitGate gate : getAllExitGates()) {
         gate.reset();
         Location c = gate.getLocation();
         for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
               for (int z = -2; z <= 2; z++) {
                  org.bukkit.block.Block b = c.clone().add(x, y, z).getBlock();
                  if (b.getType() == org.bukkit.Material.IRON_DOOR) {
                     org.bukkit.block.data.Openable door = (org.bukkit.block.data.Openable) b.getBlockData();
                     if (door.isOpen()) {
                        door.setOpen(false);
                        b.setBlockData(door);
                     }
                  }
               }
            }
         }
      }
   }

   public void createExitGate(Location loc, String mapName) {
      mapGates.computeIfAbsent(mapName, k -> new ArrayList<>());

      ExitGate newGate = new ExitGate(loc.getBlock().getLocation());
      mapGates.get(mapName).add(newGate);

      if (mapName.equalsIgnoreCase(currentMapName)) {
         activeGates.add(newGate);
      }

      saveGates();
   }

   public void createExitGate(Location loc) {
      // Fallback for old method
      createExitGate(loc, "Brak");
   }

   public boolean removeExitGate(Location loc) {
      boolean removed = false;
      for (List<ExitGate> gates : mapGates.values()) {
         Iterator<ExitGate> it = gates.iterator();
         while (it.hasNext()) {
            ExitGate gate = it.next();
            if (gate.getLocation().getBlock().equals(loc.getBlock())) {
               it.remove();
               activeGates.remove(gate);
               removed = true;
            }
         }
      }
      if (removed)
         saveGates();
      return removed;
   }

   public ExitGate getExitGateAt(Location loc) {
      for (ExitGate gate : getAllExitGates()) {
         if (gate.getLocation().getBlock().equals(loc.getBlock())) {
            return gate;
         }
      }
      return null;
   }

   public Collection<ExitGate> getAllExitGates() {
      Set<ExitGate> all = new HashSet<>(activeGates);
      for (List<ExitGate> gates : mapGates.values()) {
         all.addAll(gates);
      }
      return all;
   }

   public Collection<ExitGate> getAllGates() {
      return getAllExitGates();
   }

   public int getOpenCount() {
      return (int) activeGates.stream().filter(ExitGate::isOpened).count();
   }

   public void openExitGate(ExitGate gate) {
      gate.open();
   }

   private Location parseLocation(String s) {
      if (s == null)
         return null;
      String[] p = s.split(",");
      if (p.length < 4)
         return null;
      try {
         return new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
               Integer.parseInt(p[3]));
      } catch (Exception e) {
         return null;
      }
   }
}
