package pl.dbd.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AFKManager implements Listener {

    private final DBDPlugin plugin;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();
    private final Map<UUID, Integer> afkWarnings = new HashMap<>();
    private final File dataFile;
    private FileConfiguration data;
    
    private static final long AFK_THRESHOLD = 120000;
    private static final long WARNING_INTERVAL = 30000;

    public AFKManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "afk.yml");
        load();
        startAFKChecker();
    }

    private void load() {
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String uuidStr : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Object obj = data.get("afk-warnings." + uuid);
                List<Integer> list = (obj instanceof List) ? (List<Integer>) obj : new ArrayList<>();
                if (!list.isEmpty()) {
                    afkWarnings.put(uuid, list.get(0));
                }
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        data.set("afk-warnings", null);
        for (Map.Entry<UUID, Integer> entry : afkWarnings.entrySet()) {
            data.set("afk-warnings." + entry.getKey(), entry.getValue());
        }
        try { data.save(dataFile); }
        catch (IOException e) { plugin.getLogger().warning("[AFK] Błąd zapisu: " + e.getMessage()); }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() 
         && from.getBlockY() == to.getBlockY() 
         && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        lastLocations.put(uuid, to);
        lastMoveTime.put(uuid, System.currentTimeMillis());
        
        if (afkWarnings.containsKey(uuid)) {
            afkWarnings.remove(uuid);
            save();
        }
    }

    private void startAFKChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.getGameManager().getGameState() != GameManager.GameState.IN_GAME) continue;
                    if (!plugin.getGameManager().isSurvivor(player) 
                     && !plugin.getGameManager().isKiller(player)) continue;

                    UUID uuid = player.getUniqueId();
                    Long lastMove = lastMoveTime.get(uuid);
                    
                    if (lastMove == null) {
                        lastMoveTime.put(uuid, now);
                        continue;
                    }
                    
                    long afkTime = now - lastMove;
                    
                    if (afkTime > AFK_THRESHOLD) {
                        handleAFK(player);
                    } else if (afkTime > WARNING_INTERVAL) {
                        int warnings = afkWarnings.getOrDefault(uuid, 0);
                        if (warnings == 0) {
                            player.sendMessage("§e⚠ AFK Warning! Porusz się lub zostaniesz wyrzucony!");
                            afkWarnings.put(uuid, 1);
                            save();
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    private void handleAFK(Player player) {
        // NAPRAWIONE: kickPlayer zamiast removePlayer
        plugin.getGameManager().kickPlayer(player);
        player.sendMessage("§c✗ Zostałeś usunięty z gry z powodu AFK!");
        
        Location lobby = plugin.getGameManager().getLobbySpawn();
        if (lobby != null) {
            player.teleport(lobby);
        }
        
        Bukkit.broadcastMessage("§e" + player.getName() + " §7został usunięty z gry (AFK)");
        
        afkWarnings.remove(player.getUniqueId());
        lastMoveTime.remove(player.getUniqueId());
        lastLocations.remove(player.getUniqueId());
        save();
    }

    public void reset(Player player) {
        UUID uuid = player.getUniqueId();
        lastMoveTime.put(uuid, System.currentTimeMillis());
        afkWarnings.remove(uuid);
        save();
    }
    
    // DODANE: metody wywoływane z GameManager
    public void onGameStart() {
        lastMoveTime.clear();
        afkWarnings.clear();
        lastLocations.clear();
    }
    
    public void onGameEnd() {
        lastMoveTime.clear();
        afkWarnings.clear();
        lastLocations.clear();
    }
}