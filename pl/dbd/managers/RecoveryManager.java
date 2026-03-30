package pl.dbd.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RecoveryManager {
    private final DBDPlugin plugin;
    private BukkitTask task;

    private final Map<UUID, BossBar> recoveryBars = new HashMap<>();
    private final Map<UUID, Integer> recoveryProgress = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    private final int MAX_PROGRESS = 100;
    private final int MAX_HEAL_LIMIT = 95; // Only recover up to 95%
    private final int TICKS_WAIT_BEFORE_HEAL = 20; // Need to stand still for 1s to start

    // Tracks how long they've been standing still
    private final Map<UUID, Integer> idleTicks = new HashMap<>();

    public RecoveryManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null)
            task.cancel();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getGameState() != pl.dbd.game.GameManager.GameState.IN_GAME) {
                    clearAll();
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();

                    if (!plugin.getGameManager().isSurvivor(player)) {
                        removeBar(uuid);
                        continue;
                    }

                    PlayerStateManager.PlayerState state = plugin.getStateManager().getState(player);
                    if (state != PlayerStateManager.PlayerState.DOWNED) {
                        removeBar(uuid);
                        continue;
                    }

                    Location loc = player.getLocation();
                    Location lastLoc = lastLocations.get(uuid);

                    if (lastLoc != null && loc.getWorld().equals(lastLoc.getWorld())
                            && loc.distanceSquared(lastLoc) < 0.01) {
                        // Player is standing still
                        int idle = idleTicks.getOrDefault(uuid, 0) + 5;
                        idleTicks.put(uuid, idle);

                        if (idle >= TICKS_WAIT_BEFORE_HEAL) {
                            int progress = recoveryProgress.getOrDefault(uuid, 0) + 1; // 1% per 5 ticks = 500 ticks =
                                                                                       // 25s for 100%
                            if (progress > MAX_HEAL_LIMIT) {
                                progress = MAX_HEAL_LIMIT;
                            }
                            recoveryProgress.put(uuid, progress);
                            updateBar(player, progress);
                        }
                    } else {
                        // Player moved
                        idleTicks.put(uuid, 0);
                        // Do not reset progress, but hide the bar or keep it?
                        // Let's keep it but stop adding progress.
                        int currentProgress = recoveryProgress.getOrDefault(uuid, 0);
                        if (currentProgress > 0) {
                            updateBar(player, currentProgress);
                        } else {
                            removeBar(uuid);
                        }
                    }
                    lastLocations.put(uuid, loc);
                }
            }
        }.runTaskTimer(plugin, 5L, 5L); // Run every 5 ticks (1/4 second)
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clearAll();
    }

    private void updateBar(Player player, int progress) {
        UUID uuid = player.getUniqueId();
        BossBar bar = recoveryBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar("§aLeczenie... (" + progress + "%)", BarColor.GREEN, BarStyle.SOLID);
            bar.addPlayer(player);
            recoveryBars.put(uuid, bar);
        } else {
            bar.setTitle("§aLeczenie... (" + progress + "%)");
            bar.setProgress(progress / (double) MAX_PROGRESS);
        }
    }

    private void removeBar(UUID uuid) {
        BossBar bar = recoveryBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        // Don't remove progress immediately so they don't lose it if they inch forward?
        // Let's keep progress until they are no longer downed.
        idleTicks.remove(uuid);
    }

    public void resetProgress(UUID uuid) {
        recoveryProgress.remove(uuid);
        removeBar(uuid);
    }

    private void clearAll() {
        for (BossBar bar : recoveryBars.values()) {
            bar.removeAll();
        }
        recoveryBars.clear();
        recoveryProgress.clear();
        lastLocations.clear();
        idleTicks.clear();
    }

    public int getProgress(UUID uuid) {
        return recoveryProgress.getOrDefault(uuid, 0);
    }
}
