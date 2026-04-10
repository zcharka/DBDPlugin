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
   private final Set<UUID> hookDeadPlayers;
   private final List<UUID> queuedPlayers;

   private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

   private Location lobbySpawn;
   private Location killedSpawn;
   private Location escapedSpawn;
   private Location hookedSpawn;

   private final Map<String, GameMap> maps;
   private final List<Location> readyBlocks;
   private String currentMapName;
   private BukkitRunnable gameTimer;
   private BukkitRunnable countdownTimer;
   private BukkitRunnable inGameActionbarTask;
   public Location tempSurvSpawn;
   public String tempMapName;

   private org.bukkit.scoreboard.Scoreboard gameScoreboard;
   private org.bukkit.scoreboard.Team hookedTeam;
   private boolean lastSurvivorNotified = false;
   private boolean is2v8Mode;

   public GameManager(DBDPlugin plugin) {
      this.gameState = GameManager.GameState.LOBBY;
      this.timeLeft = 900;
      this.survivors = new HashSet<>();
      this.killers = new HashSet<>();
      this.readyPlayers = new HashSet<>();
      this.escapedPlayers = new HashSet<>();
      this.deadPlayers = new HashSet<>();
      this.hookDeadPlayers = new HashSet<>();
      this.queuedPlayers = new ArrayList<>();
      this.maps = new HashMap<>();
      this.readyBlocks = new ArrayList<>();
      this.currentMapName = "Brak";
      this.tempSurvSpawn = null;
      this.tempMapName = null;
      this.plugin = plugin;
      this.is2v8Mode = plugin.getConfig().getBoolean("game-mode-2v8", false);
      this.initScoreboard();
      this.loadMaps();
      this.startLobbyTasks();
      this.startInGameRoleTask();
      this.startStateEnforcementTask();
   }

   public boolean is2v8Mode() {
      return is2v8Mode;
   }

   public void set2v8Mode(boolean mode) {
      this.is2v8Mode = mode;
   }

   public int getMaxPlayers() {
      return is2v8Mode ? 10 : 5;
   }

   public int getRequiredGenerators() {
      return is2v8Mode ? 6 : 5;
   }

   public int getRequiredKillers() {
      return is2v8Mode ? 2 : 1;
   }

   @SuppressWarnings("deprecation")
   private void initScoreboard() {
      org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
      if (manager != null) {
         this.gameScoreboard = manager.getNewScoreboard();
         this.hookedTeam = this.gameScoreboard.getTeam("DBDHooked");
         if (this.hookedTeam == null) {
            this.hookedTeam = this.gameScoreboard.registerNewTeam("DBDHooked");
         }
         this.hookedTeam.setColor(org.bukkit.ChatColor.RED);
      }
   }

   public void setPlayerRedGlow(Player p, boolean glow) {
      if (glow) {
         p.setGlowing(true);
         if (this.hookedTeam != null) {
            this.hookedTeam.addEntry(p.getName());
         }
      } else {
         p.setGlowing(false);
         if (this.hookedTeam != null && this.hookedTeam.hasEntry(p.getName())) {
            this.hookedTeam.removeEntry(p.getName());
         }
      }
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
               while (var1.hasNext()) {
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
               String bar = "§e§lLOBBY: §a" + count + "§7/§a" + getMaxPlayers() + " §7graczy gotowych";
               Iterator<UUID> var3 = GameManager.this.readyPlayers.iterator();
               while (var3.hasNext()) {
                  UUID uuid = var3.next();
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null)
                     p.sendActionBar(bar);
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
               while (var1.hasNext()) {
                  Player p = var1.next();
                  String status = "";

                  if (GameManager.this.isKiller(p)) {
                     long cd = plugin.getHitSystemListener() != null
                           ? plugin.getHitSystemListener().getRemainingCooldownMs(p.getUniqueId())
                           : 0;
                     if (cd > 0) {
                        status = "§c§lCOOLDOWN: §e" + String.format("%.1f", cd / 1000.0) + "s";
                     } else {
                        status = "§c§lKILLER";
                     }
                  } else if (GameManager.this.isSurvivor(p)) {
                     PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
                     if (state == PlayerStateManager.PlayerState.HOOKED) {
                        status = "§4§l☠ WISISZ NA HAKU ☠";
                     } else if (state == PlayerStateManager.PlayerState.DOWNED) {
                        status = "§c§lPOWALONY";
                     } else if (state == PlayerStateManager.PlayerState.CARRIED) {
                        status = "§6§lNIESIONY";
                     } else if (state == PlayerStateManager.PlayerState.DEAD) {
                        status = "§8§lMARTWY";
                     } else {
                        status = "§a§lSURVIVOR";
                     }
                  } else {
                     continue;
                  }

                  p.sendActionBar(status + " §8| §7Gen: §e" + plugin.getGeneratorManager().getCompletedCount() + "/"
                        + getRequiredGenerators());
               }
               GameManager.this.checkGameEnd();
            }
         }
      };
      this.inGameActionbarTask.runTaskTimer(this.plugin, 0L, 4L);
   }

   private void startStateEnforcementTask() {
      (new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState == GameManager.GameState.IN_GAME) {
               for (UUID uuid : GameManager.this.killers) {
                  Player killer = Bukkit.getPlayer(uuid);
                  if (killer != null) {
                     if (killer.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                        if (killer.getWalkSpeed() != 0.0F)
                           killer.setWalkSpeed(0.0F);
                     } else if (killer.getWalkSpeed() == 0.0F) {
                        killer.setWalkSpeed(0.2F);
                     }
                  }
               }

               for (UUID uuid : GameManager.this.survivors) {
                  Player surv = Bukkit.getPlayer(uuid);
                  if (surv != null) {
                     PlayerStateManager.PlayerState state = plugin.getStateManager().getState(surv);
                     if (state == PlayerStateManager.PlayerState.DOWNED) {
                        surv.setSprinting(false);
                        surv.setSwimming(true);
                        surv.addPotionEffect(
                              new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 128, false, false, false));
                        if (surv.getWalkSpeed() != 0.03F)
                           surv.setWalkSpeed(0.03F);
                     } else {
                        surv.setSwimming(false);

                        if (state == PlayerStateManager.PlayerState.INJURED) {
                           if (surv.getWalkSpeed() != 0.2F)
                              surv.setWalkSpeed(0.2F);
                        } else if (state == PlayerStateManager.PlayerState.HOOKED
                              || state == PlayerStateManager.PlayerState.CARRIED
                              || state == PlayerStateManager.PlayerState.IN_LOCKER) {
                           if (surv.getWalkSpeed() != 0.0F)
                              surv.setWalkSpeed(0.0F);
                        } else {
                           if (surv.getWalkSpeed() == 0.0F || surv.getWalkSpeed() == 0.001F
                                 || surv.getWalkSpeed() == 0.15F) {
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

            p.sendMessage(pl.dbd.DBDPlugin.getMsg("aldoczyedolobby"));
            if (this.lobbySpawn != null)
               p.teleport(this.lobbySpawn);
            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

            for (UUID uuid : this.readyPlayers) {
               Player lp = Bukkit.getPlayer(uuid);
               if (lp != null)
                  lp.sendMessage("§a" + p.getName() + " §7gotowy! §8[§e" + count + "§7/§e5§8]");
            }

            if (count >= getMaxPlayers())
               this.startGame();
            else if (count >= 2) {
               for (UUID uuid : this.readyPlayers) {
                  Player lp = Bukkit.getPlayer(uuid);
                  if (lp != null && lp.hasPermission("dbd.admin")) {
                     lp.sendMessage("§e§l2+ graczy – tryb testowy możliwy.");
                     lp.sendMessage(pl.dbd.DBDPlugin.getMsg("7uyjegamestart7lubcz"));
                  }
               }
            }
         }
      } else {
         if (this.queuedPlayers.contains(p.getUniqueId())) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("7jestejuwkolejcenana"));
         } else {
            if (this.queuedPlayers.size() >= getMaxPlayers()) {
               p.sendMessage(
                     this.plugin.getConfig().getString("messages.queue-full",
                           "§cKolejka jest pełna! (max " + getMaxPlayers() + " graczy)"));
            } else {
               this.queuedPlayers.add(p.getUniqueId());
               String queued = this.plugin.getConfig().getString("messages.queue-joined",
                     "§aZostałeś dołączony do kolejki na następny mecz! §7({count}/" + getMaxPlayers() + ")");
               p.sendMessage(queued.replace("{count}", String.valueOf(this.queuedPlayers.size())));

               if (this.lobbySpawn != null)
                  p.teleport(this.lobbySpawn);
               p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            }
         }
      }
   }

   public void leaveLobby(Player p) {
      boolean wasReady = this.readyPlayers.remove(p.getUniqueId());
      boolean wasQueued = this.queuedPlayers.remove(p.getUniqueId());

      if (wasReady) {
         int count = this.readyPlayers.size();
         Bukkit.broadcastMessage(
               "§c" + p.getName() + " §7opuścił lobby. §8[§e" + count + "§7/§e" + getMaxPlayers() + "§8]");
      }
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
                     p.sendMessage(pl.dbd.DBDPlugin.getMsg("ameczsiskoczy7zostae"));
                     p.sendMessage("§7Gotowych graczy: §e" + this.readyPlayers.size() + "/5");
                  }
               }
               this.queuedPlayers.clear();
            }
         }, 60L);
      }
   }

   public void startGame() {
      if (this.gameState == GameManager.GameState.STARTING || this.gameState == GameManager.GameState.IN_GAME) {
         return;
      }
      if (this.readyPlayers.size() < 2) {
         Bukkit.broadcastMessage(pl.dbd.DBDPlugin.getMsg("game-not-enough-players"));
      } else {
         this.gameState = GameManager.GameState.STARTING;
         this.startCountdown();
      }
   }

   private void startCountdown() {
      final int[] countdown = new int[] { 10 };
      final int initialPlayers = this.readyPlayers.size();

      this.countdownTimer = new BukkitRunnable() {
         public void run() {
            if (GameManager.this.gameState != GameManager.GameState.STARTING) {
               this.cancel();
            } else if (GameManager.this.readyPlayers.size() < initialPlayers) {
               this.cancel();
               GameManager.this.gameState = GameManager.GameState.LOBBY;
               Bukkit.broadcastMessage("§c§lZatrzymano start! §7Jeden z graczy opuścił kolejkę.");
            } else if (countdown[0] <= 0) {
               this.cancel();
               GameManager.this.actuallyStartGame();
            } else {
               if (countdown[0] <= 5 || countdown[0] == 10) {
                  Bukkit.broadcastMessage("§e§lGra rozpocznie się za: §c" + countdown[0] + "s");
               }
               for (UUID uuid : GameManager.this.readyPlayers) {
                  Player p = Bukkit.getPlayer(uuid);
                  if (p != null)
                     p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, countdown[0] <= 3 ? 2.0F : 1.0F);
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
         for (UUID uuid : new java.util.ArrayList<>(this.survivors))
            this.plugin.getHookListener().resetPlayerHookCount(uuid);
      }

      Bukkit.broadcastMessage("§6§l========================================");
      Bukkit.broadcastMessage("§c§lGRA ROZPOCZĘTA! §7Mapa: §e" + this.currentMapName);
      Bukkit.broadcastMessage("§7Killer: §c" + this.getKillerName() + " §7| Survivorzy: §a" + this.getSurvivorNames());
      Bukkit.broadcastMessage("§6§l========================================");

      this.plugin.getGeneratorManager().activateMap(this.currentMapName);
      this.plugin.getExitGateManager().activateMap(this.currentMapName);
      this.plugin.getHookManager().activateMap(this.currentMapName);
      this.plugin.getChaseManager().startTask();
      this.plugin.getLockerManager().activateMap(this.currentMapName);

      this.hideAllNametags();

      this.startTimer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (this.gameState == GameManager.GameState.IN_GAME)
            this.releaseKiller();
      }, 300L);
   }

   private void assignRoles() {
      List<UUID> players = new ArrayList<>(this.readyPlayers);
      Collections.shuffle(players);

      int killersToAssign = Math.min(getRequiredKillers(), players.size() - 1);
      if (killersToAssign < 1)
         killersToAssign = 1;

      for (int i = 0; i < killersToAssign; i++) {
         UUID killerUUID = players.get(i);
         this.killers.add(killerUUID);
         Player killer = Bukkit.getPlayer(killerUUID);
         if (killer != null)
            this.setParent(killer, "killer");
      }

      for (int i = killersToAssign; i < players.size(); ++i) {
         UUID sUUID = players.get(i);
         this.survivors.add(sUUID);
         Player surv = Bukkit.getPlayer(sUUID);
         if (surv != null)
            this.setParent(surv, "survivor");
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
      for (UUID uuid : new java.util.ArrayList<>(this.killers)) {
         Player surv = Bukkit.getPlayer(uuid);
         if (surv != null) {
            if (chosenMap != null && chosenMap.getKillerSpawn() != null)
               surv.teleport(chosenMap.getKillerSpawn());
            this.setupPlayer(uuid, "§c§lKILLER");
            surv.sendMessage(pl.dbd.DBDPlugin.getMsg("game-killer"));
            surv.sendMessage("§7Oślepienie + blokada ruchu na 15s");
            this.plugin.getGeneratorManager().updateAurasForPlayer(surv, true);
         }
      }

      for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
         Player surv = Bukkit.getPlayer(uuid);
         if (surv != null) {
            if (chosenMap != null && chosenMap.getSurvivorSpawn() != null)
               surv.teleport(chosenMap.getSurvivorSpawn());
            this.setupPlayer(uuid, "§a§lSURVIVOR");
            surv.sendMessage(pl.dbd.DBDPlugin.getMsg("game-survivor"));
            surv.sendMessage(pl.dbd.DBDPlugin.getMsg("game-survivor-tip"));
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

         org.bukkit.inventory.ItemStack[] contents = p.getInventory().getContents();
         org.bukkit.inventory.ItemStack[] clonedContents = new org.bukkit.inventory.ItemStack[contents.length];
         for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
               clonedContents[i] = contents[i].clone();
            }
         }
         this.savedInventories.put(uuid, clonedContents);
         p.getInventory().clear();

         if (plugin.getEquipmentManager() != null) {
            pl.dbd.equipment.EqEntry.Role targetRole = roleName.contains("KILLER")
                  ? pl.dbd.equipment.EqEntry.Role.KILLER
                  : pl.dbd.equipment.EqEntry.Role.SURVIVOR;
            plugin.getEquipmentManager().giveMatchEquipment(p, targetRole);

            List<String> perks = plugin.getEquipmentManager().getEquippedPerks(p, targetRole);
            if (perks != null && !perks.isEmpty()) {
               List<String> perkNames = new java.util.ArrayList<>();
               for (String perkId : perks) {
                  pl.dbd.equipment.EqEntry e = plugin.getEquipmentManager().getEntry(perkId);
                  perkNames.add(e != null ? e.display() : perkId);
               }
               p.sendMessage("§dAktywne perki w tym meczu: §f" + String.join(", ", perkNames));
            }
         }

         p.updateInventory();
      }
   }

   private void releaseKiller() {
      for (UUID uuid : new java.util.ArrayList<>(this.killers)) {
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
               if (GameManager.this.timeLeft == 300)
                  Bukkit.broadcastMessage("§c§l5 minut!");
               if (GameManager.this.timeLeft == 60)
                  Bukkit.broadcastMessage("§c§l§n1 minuta!");
            }
         }
      };
      this.gameTimer.runTaskTimer(this.plugin, 20L, 20L);
   }

   public void checkGameEnd() {
      if (this.gameState != GameState.IN_GAME)
         return;

      int activeSurvivors = 0;
      int nonHookedSurvivors = 0;
      int standingSurvivors = 0;

      for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
         if (!this.deadPlayers.contains(uuid) && !this.escapedPlayers.contains(uuid)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
               activeSurvivors++;
               if (!plugin.getStateManager().isHooked(p)) {
                  nonHookedSurvivors++;
               }
               pl.dbd.state.PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
               if (state != pl.dbd.state.PlayerStateManager.PlayerState.HOOKED
                     && state != pl.dbd.state.PlayerStateManager.PlayerState.DOWNED
                     && state != pl.dbd.state.PlayerStateManager.PlayerState.CARRIED) {
                  standingSurvivors++;
               }
            }
         }
      }

      if (activeSurvivors == 0 && this.survivors.size() > 0 && this.escapedPlayers.isEmpty()) {
         this.endGame(GameEndReason.ALL_DEAD);
      } else if (activeSurvivors > 0 && nonHookedSurvivors == 0) {
         this.endGame(GameEndReason.ALL_DEAD);
      } else if (activeSurvivors > 0 && standingSurvivors == 0) {
         this.endGame(GameEndReason.ALL_DEAD);
      }

      if (activeSurvivors == 1 && this.gameState == GameState.IN_GAME && !this.lastSurvivorNotified) {
         this.lastSurvivorNotified = true;

         // Uruchamiamy mrugającą aurę dla ostatniego ocalałego
         if (plugin.getExitGateManager() != null) {
            plugin.getExitGateManager().triggerGatesAura();
         }

         for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
            if (!this.deadPlayers.contains(uuid) && !this.escapedPlayers.contains(uuid)) {
               Player lastSurv = Bukkit.getPlayer(uuid);
               if (lastSurv != null) {
                  lastSurv.sendMessage("§6§l==============================================");
                  lastSurv.sendMessage("§e§lJesteś ostatnim ocalałym!");
                  lastSurv.sendMessage("§aMożesz otworzyć drzwi wyjściowe BEZ kończenia generatorów!");
                  lastSurv.sendMessage("§6§l==============================================");
                  lastSurv.sendTitle("§e§lOSTATNI OCALAŁY!", "§aMożesz uciec przez exit gate!", 10, 60, 20);
               }
               break;
            }
         }
      }
   }

   public void endGame(GameManager.GameEndReason reason) {
      if (this.gameState != GameManager.GameState.ENDED) {
         this.gameState = GameManager.GameState.ENDED;
         if (this.gameTimer != null)
            this.gameTimer.cancel();
         if (this.countdownTimer != null)
            this.countdownTimer.cancel();

         Bukkit.broadcastMessage("§6§l========================================");
         Bukkit.broadcastMessage("§c§lGRA ZAKOŃCZONA! §7" + this.translateReason(reason));
         Bukkit.broadcastMessage("§6§l========================================");

         if (this.plugin.getHookListener() != null)
            this.plugin.getHookListener().cleanupAllHooks();

         pl.dbd.managers.PlayerDataManager pdm = this.plugin.getPlayerDataManager();

         if (reason == GameManager.GameEndReason.ALL_DEAD || reason == GameManager.GameEndReason.TIME_UP) {
            for (UUID uuid : new java.util.ArrayList<>(this.killers)) {
               pdm.addWin(uuid);
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                  int souls = this.plugin.getConfig().getInt("souls-rewards.match-killer-win", 5);
                  this.dispatchSouls(p, souls, "Wygrana killera");
               }
            }
            for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
               pdm.addLoss(uuid);
            }
         } else if (reason == GameManager.GameEndReason.SURVIVORS_ESCAPED) {
            for (UUID uuid : new java.util.ArrayList<>(this.killers)) {
               pdm.addLoss(uuid);
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                  int souls = this.plugin.getConfig().getInt("souls-rewards.match-killer-loss", 0);
                  this.dispatchSouls(p, souls, "Porażka killera");
               }
            }
            for (UUID uuid : this.escapedPlayers) {
               pdm.addWin(uuid);
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                  int souls = this.plugin.getConfig().getInt("souls-rewards.match-survivor-escape", 5);
                  this.dispatchSouls(p, souls, "Ucieczka z meczu");
               }
            }
            for (UUID uuid : this.deadPlayers) {
               if (this.survivors.contains(uuid))
                  pdm.addLoss(uuid);
            }
         } else if (reason == GameManager.GameEndReason.KILLER_DISCONNECTED) {
            for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
               pdm.addWin(uuid);
               Player p = Bukkit.getPlayer(uuid);
               if (p != null) {
                  int souls = this.plugin.getConfig().getInt("souls-rewards.match-survivor-walkover", 5);
                  this.dispatchSouls(p, souls, "Ucieczka przez walkowera");
               }
            }
         }

         this.plugin.getChaseManager().stopTask();
         this.plugin.getRecoveryManager().stop();
         this.plugin.getGeneratorManager().deactivateMap();
         this.plugin.getExitGateManager().deactivateMap();
         this.plugin.getHookManager().deactivateMap();
         this.plugin.getLockerManager().deactivateMap();

         if (this.plugin.getSpectateCommand() != null) {
            this.plugin.getSpectateCommand().stopAllSpectating();
         }

         this.resetGame();
      }
   }

   private void dispatchSouls(Player player, int amount, String reason) {
      if (player == null || amount <= 0)
         return;
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
      p.setPlayerListName((String) null);
      p.setCollidable(true);

      org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
      if (manager != null) {
         p.setScoreboard(manager.getMainScoreboard());
      }

      if (p.getGameMode() == GameMode.SPECTATOR) {
         p.setGameMode(GameMode.ADVENTURE);
         p.setSpectatorTarget((Entity) null);
      }

      this.plugin.getStateManager().heal(p);
   }

   private void resetGame() {
      try {
         for (UUID killerUUID : new HashSet<>(this.killers)) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && this.plugin.getCarrySystem() != null
                  && this.plugin.getCarrySystem().isCarrying(killer)) {
               this.plugin.getCarrySystem().stopCarrying(killer);
            }
         }

         if (this.plugin.getGeneratorManager() != null) {
            this.plugin.getGeneratorManager().resetAllGenerators();
         }

         if (this.plugin.getChaseManager() != null) {
            this.plugin.getChaseManager().stopTask();
         }

         if (this.plugin.getRecoveryManager() != null) {
            this.plugin.getRecoveryManager().stop();
         }

         Set<UUID> participants = new HashSet<>();
         participants.addAll(this.survivors);
         participants.addAll(this.killers);
         participants.addAll(this.escapedPlayers);
         participants.addAll(this.deadPlayers);
         participants.addAll(this.hookDeadPlayers);
         participants.addAll(this.readyPlayers);

         for (UUID pid : participants) {
            Player p = Bukkit.getPlayer(pid);
            if (p == null || !p.isOnline())
               continue;
            try {
               this.clearParent(p);
               if (p.isPermissionSet("dbd.killer"))
                  p.addAttachment(this.plugin, "dbd.killer", false);
               if (p.isPermissionSet("dbd.survivor"))
                  p.addAttachment(this.plugin, "dbd.survivor", false);

               this.clearPlayerEffects(p);

               if (this.savedInventories.containsKey(p.getUniqueId())) {
                  p.getInventory().clear();
                  p.getInventory().setContents(this.savedInventories.get(p.getUniqueId()));
               }
               p.updateInventory();

               Location target = this.lobbySpawn;
               org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld("lobby");

               if (lobbyWorld != null) {
                  target = new org.bukkit.Location(lobbyWorld, 0.5, 1.0, -0.5);
               }

               if (isDead(p) || hasEscaped(p)) {
                  if (isDead(p)) {
                     if (isHookDead(p)) {
                        p.sendMessage("§cTrafiłeś do strefy dla zawieszonych na haku!");
                        if (this.hookedSpawn != null)
                           target = this.hookedSpawn;
                        else if (this.killedSpawn != null)
                           target = this.killedSpawn;
                     } else {
                        p.sendMessage(pl.dbd.DBDPlugin.getMsg("ctrafiedostrefydlapo"));
                        if (this.killedSpawn != null)
                           target = this.killedSpawn;
                     }
                     if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                              "lp user " + p.getName() + " parent add default");
                     }
                  } else {
                     p.sendMessage(pl.dbd.DBDPlugin.getMsg("agratulacjetrafiedos"));
                     if (this.escapedSpawn != null)
                        target = this.escapedSpawn;
                     if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                              "lp user " + p.getName() + " parent add uciekli");
                     }
                  }
               }

               if (this.readyPlayers.contains(p.getUniqueId()) || this.survivors.contains(p.getUniqueId())
                     || this.killers.contains(p.getUniqueId())) {
                  if (target != null)
                     p.teleport(target);
                  else
                     Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawn " + p.getName());
               }

               if (this.plugin.getGeneratorManager() != null) {
                  this.plugin.getGeneratorManager().updateAurasForPlayer(p, false);
               }
            } catch (Exception ex) {
               this.plugin.getLogger().warning("Error resetting player " + p.getName() + ": " + ex.getMessage());
            }
         }
      } catch (Exception ex) {
         this.plugin.getLogger().warning("Error during global reset: " + ex.getMessage());
      } finally {
         Set<UUID> allInGame = new HashSet<>();
         allInGame.addAll(this.survivors);
         allInGame.addAll(this.killers);
         this.survivors.clear();
         this.killers.clear();
         this.readyPlayers.clear();
         this.escapedPlayers.clear();
         this.deadPlayers.clear();
         this.hookDeadPlayers.clear();
         this.savedInventories.clear();
         this.gameState = GameManager.GameState.LOBBY;
         this.currentMapName = "Brak";
         this.lastSurvivorNotified = false;
         this.plugin.getExitGateManager().resetAllGates();
         this.plugin.getLogger().info("[GAME] Reset zakończony. Wszyscy gracze wysłani na spawn.");

         if (this.plugin.getHookListener() != null) {
            for (UUID uuid : allInGame)
               this.plugin.getHookListener().resetPlayerHookCount(uuid);
         }

         if (this.hookedTeam != null) {
            for (String entry : new ArrayList<>(this.hookedTeam.getEntries())) {
               this.hookedTeam.removeEntry(entry);
            }
         }

         this.promoteQueue();
      }
   }

   public void clearParent(Player player) {
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
      switch (r.ordinal()) {
         case 0:
            return "Czas minął (Killer win)";
         case 1:
            return "Wszyscy uciekli";
         case 2:
            return "Killer wyszedł";
         case 3:
            return "Wszyscy martwi (Killer win)";
         default:
            return r.name();
      }
   }

   public void saveMaps() {
      File file = new File(this.plugin.getDataFolder(), "maps.yml");
      FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
      cfg.set("maps", (Object) null);
      for (Entry<String, GameMap> e : this.maps.entrySet()) {
         cfg.set("maps." + e.getKey() + ".survivorSpawn", e.getValue().getSurvivorSpawn());
         cfg.set("maps." + e.getKey() + ".killerSpawn", e.getValue().getKillerSpawn());
      }
      if (this.lobbySpawn != null)
         cfg.set("lobby", this.lobbySpawn);
      if (this.killedSpawn != null)
         cfg.set("killedSpawn", this.killedSpawn);
      if (this.escapedSpawn != null)
         cfg.set("escapedSpawn", this.escapedSpawn);
      if (this.hookedSpawn != null)
         cfg.set("hookedSpawn", this.hookedSpawn);

      try {
         cfg.save(file);
      } catch (IOException var5) {
         var5.printStackTrace();
      }
   }

   public void loadMaps() {
      File file = new File(this.plugin.getDataFolder(), "maps.yml");
      if (file.exists()) {
         FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
         if (cfg.contains("maps")) {
            for (String key : cfg.getConfigurationSection("maps").getKeys(false)) {
               Location surv = cfg.getLocation("maps." + key + ".survivorSpawn");
               Location kill = cfg.getLocation("maps." + key + ".killerSpawn");
               if (surv != null && kill != null)
                  this.maps.put(key, new GameMap(key, surv, kill));
            }
         }
         if (cfg.contains("lobby"))
            this.lobbySpawn = cfg.getLocation("lobby");
         if (cfg.contains("killedSpawn"))
            this.killedSpawn = cfg.getLocation("killedSpawn");
         if (cfg.contains("escapedSpawn"))
            this.escapedSpawn = cfg.getLocation("escapedSpawn");
         if (cfg.contains("hookedSpawn"))
            this.hookedSpawn = cfg.getLocation("hookedSpawn");
      }
   }

   public void setKilledSpawn(Location l) {
      this.killedSpawn = l;
      this.saveMaps();
   }

   public Location getKilledSpawn() {
      return this.killedSpawn;
   }

   public void setEscapedSpawn(Location l) {
      this.escapedSpawn = l;
      this.saveMaps();
   }

   public Location getEscapedSpawn() {
      return this.escapedSpawn;
   }

   public void setHookedSpawn(Location l) {
      this.hookedSpawn = l;
      this.saveMaps();
   }

   public Location getHookedSpawn() {
      return this.hookedSpawn;
   }

   public boolean isKiller(Player p) {
      return this.killers.contains(p.getUniqueId());
   }

   public boolean isSurvivor(Player p) {
      return this.survivors.contains(p.getUniqueId());
   }

   public boolean isInGame(Player p) {
      return this.isKiller(p) || this.isSurvivor(p);
   }

   // Ukryj nicki wszystkim graczom w meczu (scoreboard team z
   // nametagVisibility=NEVER)
   private void hideAllNametags() {
      if (this.gameScoreboard == null)
         return;
      org.bukkit.scoreboard.Team hideTeam = this.gameScoreboard.getTeam("dbd_hide_names");
      if (hideTeam == null) {
         hideTeam = this.gameScoreboard.registerNewTeam("dbd_hide_names");
      }
      hideTeam.setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
            org.bukkit.scoreboard.Team.OptionStatus.NEVER);

      List<UUID> allInGame = new java.util.ArrayList<>();
      allInGame.addAll(this.killers);
      allInGame.addAll(this.survivors);
      for (UUID uuid : allInGame) {
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            hideTeam.addEntry(p.getName());
            p.setScoreboard(this.gameScoreboard); // Ustaw nasz dedykowany scoreboard
         }
      }
   }

   public boolean isDead(Player p) {
      return this.deadPlayers.contains(p.getUniqueId());
   }

   public boolean isHookDead(Player p) {
      return this.hookDeadPlayers.contains(p.getUniqueId());
   }

   public boolean hasEscaped(Player p) {
      return this.escapedPlayers.contains(p.getUniqueId());
   }

   public void markDead(Player p) {
      if (!this.deadPlayers.contains(p.getUniqueId())) {
         this.deadPlayers.add(p.getUniqueId());
      }
   }

   public void markHookDead(Player p) {
      this.hookDeadPlayers.add(p.getUniqueId());
      this.markDead(p);
   }

   public void markEscaped(Player p) {
      this.escapedPlayers.add(p.getUniqueId());
   }

   public Set<UUID> getSurvivorUUIDs() {
      return Collections.unmodifiableSet(this.survivors);
   }

   public int getActiveSurvivorsCount() {
      int active = 0;
      for (UUID uuid : new java.util.ArrayList<>(this.survivors)) {
         if (!this.deadPlayers.contains(uuid) && !this.escapedPlayers.contains(uuid)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
               active++;
            }
         }
      }
      return active;
   }

   public Set<UUID> getSurvivors() {
      return this.survivors;
   }

   public Set<UUID> getKillers() {
      return this.killers;
   }

   public Set<UUID> getReadyPlayers() {
      return new HashSet<>(this.readyPlayers);
   }

   public java.util.List<UUID> getQueuedPlayers() {
      return java.util.Collections.unmodifiableList(this.queuedPlayers);
   }

   public List<Location> getReadyBlocks() {
      return this.readyBlocks;
   }

   public void setLobbySpawn(Location l) {
      this.lobbySpawn = l;
      this.saveMaps();
   }

   public Location getLobbySpawn() {
      return this.lobbySpawn;
   }

   public void addMap(String n, Location s, Location k) {
      this.maps.put(n, new GameMap(n, s, k));
      this.saveMaps();
   }

   public void removeMap(String n) {
      this.maps.remove(n);
      this.saveMaps();
   }

   public Set<String> getMaps() {
      return this.maps.keySet();
   }

   public GameMap getMap(String name) {
      return name == null ? null : this.maps.get(name);
   }

   public String getCurrentMap() {
      return this.currentMapName;
   }

   public void forceStart() {
      this.startGame();
   }

   public GameManager.GameState getGameState() {
      return this.gameState;
   }

   public UUID getKiller() {
      if (this.killers.isEmpty())
         return null;
      return this.killers.iterator().next();
   }

   public void removePlayer(UUID uuid) {
      if (this.deadPlayers.contains(uuid))
         return; // Unikamy dublowania napisów
      this.deadPlayers.add(uuid);
      Player p = Bukkit.getPlayer(uuid);
      if (p != null) {
         this.plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.DEAD);

         // Broadcast Zabójstwa do Mordercy
         String title = this.plugin.getConfig().getString("killer-kill-title", "&c☠ ZABÓJSTWO ☠");
         String subtitle = this.plugin.getConfig().getString("killer-kill-subtitle", "&4zabiłeś {survivor}")
               .replace("{survivor}", p.getName());

         title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
         subtitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', subtitle);

         for (UUID kId : this.killers) {
            Player killer = Bukkit.getPlayer(kId);
            if (killer != null && killer.isOnline()) {
               killer.sendTitle(title, subtitle, 10, 60, 20);
               killer.playSound(killer.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.4f, 1.2f);
            }
         }
      }
      if (this.deadPlayers.size() + this.escapedPlayers.size() >= this.survivors.size()) {
         this.endGame(GameManager.GameEndReason.ALL_DEAD);
      }
   }

   public void kickPlayer(Player p) {
      UUID uuid = p.getUniqueId();
      if (this.isInGame(p)) {
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("czostaewyrzuconyzgry"));
         this.clearPlayerEffects(p);

         // PRZYWRACANIE ZAPISANEGO EKWIPUNKU PO WYRZUCENIU / WYJŚCIU Z GRY
         if (this.savedInventories.containsKey(uuid)) {
            p.getInventory().clear();
            p.getInventory().setContents(this.savedInventories.get(uuid));
            this.savedInventories.remove(uuid);
         }
         p.updateInventory();

         this.killers.remove(uuid);
         this.survivors.remove(uuid);
         if (this.killers.isEmpty() && this.gameState == GameManager.GameState.IN_GAME) {
            this.endGame(GameManager.GameEndReason.KILLER_DISCONNECTED);
         } else if (this.survivors.isEmpty() && this.gameState == GameManager.GameState.IN_GAME) {
            this.endGame(GameManager.GameEndReason.ALL_DEAD);
         }

         p.setWalkSpeed(0.2F);
         this.clearParent(p);
      }
   }

   private String getKillerName() {
      StringBuilder sb = new StringBuilder();
      int i = 0;
      java.util.Iterator<UUID> var3 = this.killers.iterator();
      while (var3.hasNext()) {
         UUID uuid = var3.next();
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            if (i++ > 0)
               sb.append(", ");
            sb.append(p.getName());
         }
      }
      return sb.length() > 0 ? sb.toString() : "Brak";
   }

   private String getSurvivorNames() {
      StringBuilder sb = new StringBuilder();
      int i = 0;
      Iterator<UUID> var3 = this.survivors.iterator();
      while (var3.hasNext()) {
         UUID uuid = var3.next();
         Player p = Bukkit.getPlayer(uuid);
         if (p != null) {
            if (i++ > 0)
               sb.append(", ");
            sb.append(p.getName());
         }
      }
      return sb.length() > 0 ? sb.toString() : "Brak";
   }

   public static enum GameState {
      LOBBY, STARTING, IN_GAME, ENDED;
   }

   public static enum GameEndReason {
      TIME_UP, SURVIVORS_ESCAPED, KILLER_DISCONNECTED, ALL_DEAD;
   }

   public void setDownedState(Player player) {
      if (this.isInGame(player) && this.isSurvivor(player)) {
         player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
         player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false));

         player.setSwimming(true);

         String msg = this.plugin.getConfig().getString("messages.game-downed",
               "§cZostałeś powalony na ziemię! Czekaj na ratunek.");
         player.sendMessage(msg);
      }
   }
}