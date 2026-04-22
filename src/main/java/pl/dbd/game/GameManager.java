package pl.dbd.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;
import pl.dbd.util.PlayerUtils; // ZMIANA PAKIETU NA UTIL

public class GameManager {
   private final DBDPlugin plugin;
   private GameManager.GameState gameState;
   private int timeLeft;
   private final Set<UUID> survivors;
   private final Set<UUID> killers;
   private final Set<UUID> readyPlayers;
   private final Set<UUID> escapedPlayers;
   private final Set<UUID> deadPlayers;
   private final List<UUID> queuedPlayers;
   
   private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

   private Location lobbySpawn;
   private Location killedSpawn;
   private Location escapedSpawn;
   
   private final Map<String, GameMap> maps;
   private final List<Location> readyBlocks;
   private String currentMapName;
   private BukkitRunnable gameTimer;
   private BukkitRunnable countdownTimer;
   private BukkitRunnable inGameActionbarTask;
   public Location tempSurvSpawn;
   public String tempMapName;

   public GameManager(DBDPlugin plugin) {
      this.gameState = GameManager.GameState.LOBBY;
      this.timeLeft = 900;
      this.survivors = new HashSet<>();
      this.killers = new HashSet<>();
      this.readyPlayers = new HashSet<>();
      this.escapedPlayers = new HashSet<>();
      this.deadPlayers = new HashSet<>();
      this.queuedPlayers = new ArrayList<>();
      this.maps = new HashMap<>();
      this.readyBlocks = new ArrayList<>();
      this.currentMapName = "Brak";
      this.tempSurvSpawn = null;
      this.tempMapName = null;
      this.plugin = plugin;
      this.loadMaps();
      this.startLobbyTasks();
      this.startInGameRoleTask();
      this.startStateEnforcementTask();
   }

   public void restoreInventoryOnQuit(Player p) {
      if (this.savedInventories.containsKey(p.getUniqueId())) {
         p.getInventory().clear();
         p.getInventory().setContents(this.savedInventories.remove(p.getUniqueId()));
      }
   }

   private void startLobbyTasks() {
      (new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState == GameManager.GameState.LOBBY) {
               Iterator<UUID> var1 = (new HashSet<>(GameManager.this.readyPlayers)).iterator();
               while(var1.hasNext()) {
                  UUID uuid = var1.next();
                  if (Bukkit.getPlayer(uuid) == null) {
                     GameManager.this.readyPlayers.remove(uuid);
                  }
               }
            }
         }
      }).runTaskTimer(this.plugin, 100L, 100L);

      (new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState == GameManager.GameState.LOBBY) {
               int count = GameManager.this.readyPlayers.size();
               String bar = "§e§lLOBBY: §a" + count + "§7/§a5 §7graczy gotowych";
               Iterator<UUID> var3 = GameManager.this.readyPlayers.iterator();
               while(var3.hasNext()) {
                  UUID uuid = var3.next();
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null) p.sendActionBar(bar);
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 20L);
   }

   private void startInGameRoleTask() {
      this.inGameActionbarTask = new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState == GameManager.GameState.IN_GAME) {
               Iterator<? extends Player> var1 = Bukkit.getOnlinePlayers().iterator();
               while(var1.hasNext()) {
                  Player p = var1.next();
                  String status = "";
                  
                  if (GameManager.this.isKiller(p)) {
                      status = "§c§lKILLER";
                  } else if (GameManager.this.isSurvivor(p)) {
                      // SPRAWDZANIE STANU (HAK, POWALONY)
                      PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
                      if (state == PlayerStateManager.PlayerState.HOOKED) {
                          status = "§4§l☠ WISISZ NA HAKU ☠";
                      } else if (state == PlayerStateManager.PlayerState.DOWNED) {
                          status = "§c§lPOWALONY";
                      } else if (state == PlayerStateManager.PlayerState.CARRIED) {
                          status = "§6§lNIESIONY";
                      } else {
                          status = "§a§lSURVIVOR";
                      }
                  } else {
                      continue;
                  }
                  
                  String map = currentMapName != null && !currentMapName.equals("Brak") ? currentMapName : "Brak mapy";
                  p.sendActionBar(status + " §8| §7Gen: §e" + plugin.getGeneratorManager().getCompletedCount() + "/" + plugin.getGeneratorManager().getTotalCount());
               }
            }
         }
      };
      this.inGameActionbarTask.runTaskTimer(this.plugin, 0L, 20L);
   }

   private void startStateEnforcementTask() {
      (new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState == GameManager.GameState.IN_GAME) {
               for(UUID uuid : GameManager.this.killers) {
                  Player killer = Bukkit.getPlayer(uuid);
                  if (killer != null) {
                     if (killer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                        if (killer.getWalkSpeed() != 0.0F) killer.setWalkSpeed(0.0F);
                     } else if (killer.getWalkSpeed() == 0.0F) {
                        killer.setWalkSpeed(0.2F);
                     }
                  }
               }

               for(UUID uuid : GameManager.this.survivors) {
                   Player surv = Bukkit.getPlayer(uuid);
                   if (surv != null) {
                       PlayerStateManager.PlayerState state = plugin.getStateManager().getState(surv);
                       if (state == PlayerStateManager.PlayerState.DOWNED) {
                           surv.setSprinting(false);
                           surv.setSwimming(true); // CZOŁGANIE
                           surv.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 250, false, false, false));
                           if (surv.getWalkSpeed() > 0.001F) surv.setWalkSpeed(0.001F);
                       } else {
                           surv.setSwimming(false);
                           
                           if (state == PlayerStateManager.PlayerState.INJURED) {
                               surv.setSprinting(false);
                               if (surv.getWalkSpeed() != 0.15F) surv.setWalkSpeed(0.15F);
                           } else if (state == PlayerStateManager.PlayerState.HOOKED || state == PlayerStateManager.PlayerState.CARRIED) {
                               if (surv.getWalkSpeed() != 0.0F) surv.setWalkSpeed(0.0F);
                           } else {
                               if (surv.getWalkSpeed() == 0.0F || surv.getWalkSpeed() == 0.001F || surv.getWalkSpeed() == 0.15F) {
                                   surv.setWalkSpeed(0.2F);
                               }
                           }
                       }
                   }
               }
            }
         }
      }).runTaskTimer(this.plugin, 0L, 2L);
   }

   public void joinLobby(Player p) {
      if (this.gameState != GameManager.GameState.IN_GAME && this.gameState != GameManager.GameState.STARTING) {
         if (this.gameState == GameManager.GameState.LOBBY) {
            this.clearPlayerEffects(p);
            this.readyPlayers.add(p.getUniqueId());
            int count = this.readyPlayers.size();
            for (UUID uuid : this.readyPlayers) {
               Player lp = Bukkit.getPlayer(uuid);
               if (lp != null) lp.sendMessage("§a" + p.getName() + " §7gotowy! §8[§e" + count + "§7/§e5§8]");
            }

            if (count >= 5) this.startGame();
            else if (count >= 2) {
               for (UUID uuid : this.readyPlayers) {
                  Player lp = Bukkit.getPlayer(uuid);
                  if (lp != null) {
                     lp.sendMessage("§e§l2+ graczy – tryb testowy możliwy.");
                     lp.sendMessage("§7Użyj §e/game start §7lub czekaj na 5.");
                  }
               }
            }
         }
      } else {
         p.sendMessage(this.plugin.getConfig().getString("messages.lobby-busy", "§cNie można użyć /gotowy podczas aktywnego meczu."));
         if (this.queuedPlayers.contains(p.getUniqueId())) {
            p.sendMessage("§7Jesteś już w kolejce na następny mecz.");
         } else {
            if (this.queuedPlayers.size() >= 5) {
               p.sendMessage(this.plugin.getConfig().getString("messages.queue-full", "§cKolejka jest pełna! (max 5 graczy)"));
            } else {
               this.queuedPlayers.add(p.getUniqueId());
               String queued = this.plugin.getConfig().getString("messages.queue-joined", "§aZostałeś dołączony do kolejki na następny mecz! §7({count}/5)");
               p.sendMessage(queued.replace("{count}", String.valueOf(this.queuedPlayers.size())));
            }
         }
      }
   }

   public void leaveLobby(Player p) {
      this.readyPlayers.remove(p.getUniqueId());
      int count = this.readyPlayers.size();
      Bukkit.broadcastMessage("§c" + p.getName() + " §7opuścił lobby. §8[§e" + count + "§7/§e5§8]");
   }

   private void promoteQueue() {
      if (!this.queuedPlayers.isEmpty()) {
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (this.lobbySpawn != null) {
               for (UUID uuid : new ArrayList<>(this.queuedPlayers)) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null && p.isOnline()) {
                     this.readyPlayers.add(uuid);
                     p.teleport(this.lobbySpawn);
                     p.sendMessage("§aMecz się skończył! §7Zostałeś przesunięty do lobby.");
                     p.sendMessage("§7Gotowych graczy: §e" + this.readyPlayers.size() + "/5");
                  }
               }
               this.queuedPlayers.clear();
            }
         }, 60L);
      }
   }

   public void startGame() {
      if (this.readyPlayers.size() < 2) {
         Bukkit.broadcastMessage(this.plugin.getMessage("game-not-enough-players"));
      } else {
         this.gameState = GameManager.GameState.STARTING;
         this.startCountdown();
      }
   }

   private void startCountdown() {
      final int[] countdown = new int[]{10};
      this.countdownTimer = new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState != GameManager.GameState.STARTING) {
               this.cancel();
            } else if (countdown[0] <= 0) {
               this.cancel();
               GameManager.this.actuallyStartGame();
            } else {
               if (countdown[0] <= 5 || countdown[0] == 10) {
                  Bukkit.broadcastMessage("§e§lGra rozpocznie się za: §c" + countdown[0] + "s");
               }
               for (UUID uuid : GameManager.this.readyPlayers) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, countdown[0] <= 3 ? 2.0F : 1.0F);
               }
               countdown[0]--;
            }
         }
      };
      this.countdownTimer.runTaskTimer(this.plugin, 0L, 20L);
   }

   private void actuallyStartGame() {
      this.assignRoles();
      List<String> mapNames = new ArrayList<>(this.maps.keySet());
      if (!mapNames.isEmpty()) {
         this.currentMapName = mapNames.get((new Random()).nextInt(mapNames.size()));
      }

      this.applyGameStart();
      this.gameState = GameManager.GameState.IN_GAME;
      this.timeLeft = 900;
      
      if (this.plugin.getHookListener() != null) {
         for (UUID uuid : this.survivors) this.plugin.getHookListener().resetPlayerHookCount(uuid);
      }
      if (this.plugin.getAfkManager() != null) this.plugin.getAfkManager().onGameStart();

      Bukkit.broadcastMessage("§6§l========================================");
      Bukkit.broadcastMessage("§c§lGRA ROZPOCZĘTA! §7Mapa: §e" + this.currentMapName);
      Bukkit.broadcastMessage("§7Killer: §c" + this.getKillerName() + " §7| Survivorzy: §a" + this.getSurvivorNames());
      Bukkit.broadcastMessage("§6§l========================================");
      
      // Aktywacja genów (ignoruje nazwę mapy w nowej wersji GeneratorManagera)
      this.plugin.getGeneratorManager().activateMap(this.currentMapName);
      
      this.startTimer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (this.gameState == GameManager.GameState.IN_GAME) this.releaseKiller();
      }, 300L);
   }

   private void assignRoles() {
      List<UUID> players = new ArrayList<>(this.readyPlayers);
      Collections.shuffle(players);
      UUID killerUUID = players.get(0);
      this.killers.add(killerUUID);
      Player killer = Bukkit.getPlayer(killerUUID);
      if (killer != null) this.setParent(killer, "killer");

      for(int i = 1; i < players.size(); ++i) {
         UUID sUUID = players.get(i);
         this.survivors.add(sUUID);
         Player surv = Bukkit.getPlayer(sUUID);
         if (surv != null) this.setParent(surv, "survivor");
      }
   }

   private void setParent(Player player, String group) {
      try {
         if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent set " + group);
         } else {
            player.addAttachment(this.plugin, "dbd." + group, true);
         }
      } catch (Exception var4) {
         this.plugin.getLogger().warning("[LP] " + var4.getMessage());
      }
   }

   private void applyGameStart() {
      GameMap chosenMap = this.getMap(this.currentMapName);
      for (UUID uuid : this.killers) {
         Player surv = Bukkit.getPlayer(uuid);
         if (surv != null) {
            if (chosenMap != null && chosenMap.getKillerSpawn() != null) surv.teleport(chosenMap.getKillerSpawn());
            this.setupPlayer(uuid, "§c§lKILLER");
            surv.sendMessage(this.plugin.getMessage("game-killer"));
            surv.sendMessage("§7Oślepienie + blokada ruchu na 15s");
         }
      }

      for (UUID uuid : this.survivors) {
         Player surv = Bukkit.getPlayer(uuid);
         if (surv != null) {
            if (chosenMap != null && chosenMap.getSurvivorSpawn() != null) surv.teleport(chosenMap.getSurvivorSpawn());
            this.setupPlayer(uuid, "§a§lSURVIVOR");
            surv.sendMessage(this.plugin.getMessage("game-survivor"));
            surv.sendMessage(this.plugin.getMessage("game-survivor-tip"));
         }
      }
   }

   private void setupPlayer(UUID uuid, String roleName) {
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) {
         p.setGameMode(GameMode.ADVENTURE);
         p.setHealth(20.0D);
         p.setFoodLevel(20);
         p.sendTitle(roleName, "§7Powodzenia!", 10, 70, 20);

         if (roleName.contains("KILLER")) {
            p.setWalkSpeed(0.0F);
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 2, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
         }

         this.savedInventories.put(uuid, p.getInventory().getContents());
         p.getInventory().clear();
         
         if (plugin.getEquipmentManager() != null) {
             pl.dbd.equipment.EqEntry.Role targetRole = roleName.contains("KILLER") ? pl.dbd.equipment.EqEntry.Role.KILLER : pl.dbd.equipment.EqEntry.Role.SURVIVOR;
             plugin.getEquipmentManager().giveMatchEquipment(p, targetRole);
             
             List<String> perks = plugin.getEquipmentManager().getEquippedPerks(p, targetRole);
             if (perks != null && !perks.isEmpty()) {
                 p.sendMessage("§dAktywne perki w tym meczu: §f" + String.join(", ", perks));
             }
         }

         p.updateInventory();
      }
   }

   private void releaseKiller() {
      for (UUID uuid : this.killers) {
         Player killer = Bukkit.getPlayer(uuid);
         if (killer != null) {
            killer.removePotionEffect(PotionEffectType.BLINDNESS);
            killer.removePotionEffect(PotionEffectType.SLOWNESS);
            killer.setWalkSpeed(0.2F);
            killer.sendTitle("§c§lWYPUSZCZONO!", "§7Poluj!", 10, 50, 20);
         }
      }
      Bukkit.broadcastMessage("§c§l⚠ KILLER WYPUSZCZONY! ⚠");
   }

   private void startTimer() {
      this.gameTimer = new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState != GameManager.GameState.IN_GAME) {
               this.cancel();
            } else if (GameManager.this.timeLeft <= 0) {
               GameManager.this.endGame(GameManager.GameEndReason.TIME_UP);
               this.cancel();
            } else {
               --GameManager.this.timeLeft;
               if (GameManager.this.timeLeft % 60 == 0 && GameManager.this.timeLeft > 0) {
                  Bukkit.broadcastMessage("§ePozostało: §c" + GameManager.this.timeLeft / 60 + " min");
               }
               if (GameManager.this.timeLeft == 300) Bukkit.broadcastMessage("§c§l5 minut!");
               if (GameManager.this.timeLeft == 60) Bukkit.broadcastMessage("§c§l§n1 minuta!");
            }
         }
      };
      this.gameTimer.runTaskTimer(this.plugin, 20L, 20L);
   }

   public void endGame(GameManager.GameEndReason reason) {
      if (this.gameState != GameManager.GameState.ENDED) {
         this.gameState = GameManager.GameState.ENDED;
         if (this.gameTimer != null) this.gameTimer.cancel();
         if (this.countdownTimer != null) this.countdownTimer.cancel();

         Bukkit.broadcastMessage("§6§l========================================");
         Bukkit.broadcastMessage("§c§lGRA ZAKOŃCZONA! §7" + this.translateReason(reason));
         Bukkit.broadcastMessage("§6§l========================================");
         
         if (this.plugin.getHookListener() != null) this.plugin.getHookListener().cleanupAllHooks();
         if (this.plugin.getAfkManager() != null) this.plugin.getAfkManager().onGameEnd();

         if (reason != GameManager.GameEndReason.SURVIVORS_ESCAPED && reason != GameManager.GameEndReason.TIME_UP) {
            if (reason == GameManager.GameEndReason.ALL_DEAD) {
               for (UUID uuid : this.killers) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null) this.dispatchSouls(p, 5, "Wygrana killera");
               }
            }
         } else {
            for (UUID uuid : this.killers) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) this.dispatchSouls(p, 0, "Porażka killera");
            }
            for (UUID uuid : this.escapedPlayers) {
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) this.dispatchSouls(p, 5, "Ucieczka z meczu");
            }
         }

         this.plugin.getGeneratorManager().deactivateMap();
         this.resetGame();
      }
   }

   private void dispatchSouls(Player player, int amount, String reason) {
      if (player == null || amount <= 0) return;
      String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
              cmd.replace("{player}", player.getName())
                 .replace("{amount}", String.valueOf(amount))
                 .replace("{reason}", reason.replace(" ", "_")));
   }

   private void clearPlayerEffects(Player p) {
      PlayerUtils.resetPlayerState(p);

      for (PotionEffect effect : p.getActivePotionEffects()) {
         p.removePotionEffect(effect.getType());
      }

      p.setWalkSpeed(0.2F);
      p.setFlySpeed(0.1F);
      p.setAllowFlight(false);
      p.setFlying(false);
      p.setSwimming(false);
      p.setGliding(false);
      p.setSprinting(false);
      p.setGlowing(false);
      p.setPlayerListName((String)null);
      
      p.getInventory().clear();
      if (this.savedInventories.containsKey(p.getUniqueId())) {
         p.getInventory().setContents(this.savedInventories.remove(p.getUniqueId()));
      }
      
      if (p.getGameMode() == GameMode.SPECTATOR) {
         p.setGameMode(GameMode.ADVENTURE);
         p.setSpectatorTarget((Entity)null);
      }

      this.plugin.getStateManager().heal(p);
   }

   private void resetGame() {
      for (UUID killerUUID : new HashSet<>(this.killers)) {
         Player killer = Bukkit.getPlayer(killerUUID);
         if (killer != null && this.plugin.getCarrySystem().isCarrying(killer)) {
            this.plugin.getCarrySystem().stopCarrying(killer);
         }
      }

      // RESET GENERATORÓW PO MECZU
      this.plugin.getGeneratorManager().resetAllGenerators();
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "exitgate reset");
      
      for (Player p : Bukkit.getOnlinePlayers()) {
         this.clearParent(p);
         if (p.isPermissionSet("dbd.killer")) p.addAttachment(this.plugin, "dbd.killer", false);
         if (p.isPermissionSet("dbd.survivor")) p.addAttachment(this.plugin, "dbd.survivor", false);

         this.clearPlayerEffects(p);
         
         Location target = this.lobbySpawn;
         
         if (isDead(p) && this.killedSpawn != null) {
             target = this.killedSpawn;
             p.sendMessage("§cTrafiłeś do strefy dla poległych.");
         } else if (hasEscaped(p) && this.escapedSpawn != null) {
             target = this.escapedSpawn;
             p.sendMessage("§aGratulacje! Trafiłeś do strefy zwycięzców.");
         }

         if (target != null) p.teleport(target);
         else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
      }

      Set<UUID> allInGame = new HashSet<>();
      allInGame.addAll(this.survivors);
      allInGame.addAll(this.killers);
      this.survivors.clear();
      this.killers.clear();
      this.readyPlayers.clear();
      this.escapedPlayers.clear();
      this.deadPlayers.clear();
      this.savedInventories.clear();
      this.gameState = GameManager.GameState.LOBBY;
      this.currentMapName = "Brak";
      this.plugin.getLogger().info("[GAME] Reset zakończony. Wszyscy gracze wysłani na spawn.");
      
      if (this.plugin.getHookListener() != null) {
         for (UUID uuid : allInGame) this.plugin.getHookListener().resetPlayerHookCount(uuid);
      }

      this.promoteQueue();
   }

   private void clearParent(Player player) {
      try {
         if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            String name = player.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + name + " parent remove killer");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + name + " parent remove survivor");
            String defaultGroup = this.plugin.getConfig().getString("luckperms.default-group", "default");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + name + " parent set " + defaultGroup);
         }
      } catch (Exception var4) {
         this.plugin.getLogger().warning("[LP] " + var4.getMessage());
      }
   }

   private String translateReason(GameManager.GameEndReason r) {
      switch(r.ordinal()) {
      case 0: return "Czas minął (Killer win)";
      case 1: return "Wszyscy uciekli";
      case 2: return "Killer wyszedł";
      case 3: return "Wszyscy martwi (Killer win)";
      default: return r.name();
      }
   }

   public void saveMaps() {
      File file = new File(this.plugin.getDataFolder(), "maps.yml");
      FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
      cfg.set("maps", (Object)null);
      for (Entry<String, GameMap> e : this.maps.entrySet()) {
         cfg.set("maps." + e.getKey() + ".survivorSpawn", e.getValue().getSurvivorSpawn());
         cfg.set("maps." + e.getKey() + ".killerSpawn", e.getValue().getKillerSpawn());
      }
      if (this.lobbySpawn != null) cfg.set("lobby", this.lobbySpawn);
      if (this.killedSpawn != null) cfg.set("killedSpawn", this.killedSpawn);
      if (this.escapedSpawn != null) cfg.set("escapedSpawn", this.escapedSpawn);
      
      try { cfg.save(file); } catch (IOException var5) { var5.printStackTrace(); }
   }

   private void loadMaps() {
      File file = new File(this.plugin.getDataFolder(), "maps.yml");
      if (file.exists()) {
         FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
         if (cfg.contains("maps")) {
            for (String key : cfg.getConfigurationSection("maps").getKeys(false)) {
               Location surv = cfg.getLocation("maps." + key + ".survivorSpawn");
               Location kill = cfg.getLocation("maps." + key + ".killerSpawn");
               if (surv != null && kill != null) this.maps.put(key, new GameMap(key, surv, kill));
            }
         }
         if (cfg.contains("lobby")) this.lobbySpawn = cfg.getLocation("lobby");
         if (cfg.contains("killedSpawn")) this.killedSpawn = cfg.getLocation("killedSpawn");
         if (cfg.contains("escapedSpawn")) this.escapedSpawn = cfg.getLocation("escapedSpawn");
      }
   }
   
   public void setKilledSpawn(Location l) { this.killedSpawn = l; this.saveMaps(); }
   public void setEscapedSpawn(Location l) { this.escapedSpawn = l; this.saveMaps(); }

   public boolean isKiller(Player p) { return this.killers.contains(p.getUniqueId()); }
   public boolean isSurvivor(Player p) { return this.survivors.contains(p.getUniqueId()); }
   public boolean isInGame(Player p) { return this.isKiller(p) || this.isSurvivor(p); }
   public boolean isDead(Player p) { return this.deadPlayers.contains(p.getUniqueId()); }
   public boolean hasEscaped(Player p) { return this.escapedPlayers.contains(p.getUniqueId()); }
   public void markDead(Player p) { this.deadPlayers.add(p.getUniqueId()); }
   public void markEscaped(Player p) { this.escapedPlayers.add(p.getUniqueId()); }
   public Set<UUID> getSurvivorUUIDs() { return Collections.unmodifiableSet(this.survivors); }
   public Set<UUID> getReadyPlayers() { return new HashSet<>(this.readyPlayers); }
   public List<Location> getReadyBlocks() { return this.readyBlocks; }
   public void setLobbySpawn(Location l) { this.lobbySpawn = l; this.saveMaps(); }
   public Location getLobbySpawn() { return this.lobbySpawn; }
   public void addMap(String n, Location s, Location k) { this.maps.put(n, new GameMap(n, s, k)); this.saveMaps(); }
   public void removeMap(String n) { this.maps.remove(n); this.saveMaps(); }
   public Set<String> getMaps() { return this.maps.keySet(); }
   public GameMap getMap(String name) { return name == null ? null : this.maps.get(name); }
   public String getCurrentMap() { return this.currentMapName; }
   public void forceStart() { this.startGame(); }
   public GameManager.GameState getGameState() { return this.gameState; }
   public UUID getKiller() { if (this.killers.isEmpty()) return null; return this.killers.iterator().next(); }

   public void removePlayer(UUID uuid) {
      this.deadPlayers.add(uuid);
      if (this.deadPlayers.size() + this.escapedPlayers.size() >= this.survivors.size()) {
         this.endGame(GameManager.GameEndReason.ALL_DEAD);
      }
   }

   public void kickPlayer(Player p) {
      UUID uuid = p.getUniqueId();
      if (this.isInGame(p)) {
         p.sendMessage("§cZostałeś wyrzucony z gry.");
         this.clearPlayerEffects(p);
         this.killers.remove(uuid);
         this.survivors.remove(uuid);
         if (this.killers.isEmpty() && this.gameState == GameManager.GameState.IN_GAME) {
            this.endGame(GameManager.GameEndReason.KILLER_DISCONNECTED);
         }
      }
   }

   private String getKillerName() {
      Iterator<UUID> var1 = this.killers.iterator();
      Player p;
      do {
         if (!var1.hasNext()) return "Brak";
         UUID uuid = var1.next();
         p = Bukkit.getPlayer(uuid);
      } while(p == null);
      return p.getName();
   }

   private String getSurvivorNames() {
      StringBuilder sb = new StringBuilder();
      int i = 0;
      Iterator<UUID> var3 = this.survivors.iterator();
      while(var3.hasNext()) {
         UUID uuid = var3.next();
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            if (i++ > 0) sb.append(", ");
            sb.append(p.getName());
         }
      }
      return sb.length() > 0 ? sb.toString() : "Brak";
   }

   public static enum GameState { LOBBY, STARTING, IN_GAME, ENDED; }
   public static enum GameEndReason { TIME_UP, SURVIVORS_ESCAPED, KILLER_DISCONNECTED, ALL_DEAD; }

   public void setDownedState(Player player) {
      if (this.isInGame(player) && this.isSurvivor(player)) {
         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
         player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false));
         
         player.setSwimming(true); 
         
         String msg = this.plugin.getConfig().getString("messages.game-downed", "§cZostałeś powalony na ziemię! Czekaj na ratunek.");
         player.sendMessage(msg);
      }
   }
}