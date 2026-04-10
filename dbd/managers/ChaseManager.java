package pl.dbd.managers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import pl.dbd.state.PlayerStateManager;
import pl.dbd.state.PlayerStateManager.PlayerState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChaseManager {
    private final DBDPlugin plugin;
    private BukkitTask chaseTask;

    // Track which survivor is being chased by which killer
    // Survivor UUID -> Killer UUID
    private final Map<UUID, UUID> activeChases = new HashMap<>();

    // Survivor UUID -> ticks lost
    private final Map<UUID, Integer> chaseLostCounters = new HashMap<>();

    // Track total duration of chase to loop music
    private final Map<UUID, Integer> chaseTimeCounters = new HashMap<>();

    private final double CHASE_START_DISTANCE = 16.0;
    private final double CHASE_END_DISTANCE = 24.0;
    private final int MAX_LOST_TICKS = 100; // 5 seconds (20 ticks * 5)
    private final double FOV_ANGLE = 0.6; // Dot product threshold for "looking at"

    public ChaseManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        if (chaseTask != null) {
            chaseTask.cancel();
        }

        chaseTask = new BukkitRunnable() {
            @Override
            public void run() {
                GameManager gm = plugin.getGameManager();
                if (gm.getGameState() != GameManager.GameState.IN_GAME) {
                    return;
                }

                // Temporary map to track who is seen this tick
                Map<UUID, UUID> currentlySeen = new HashMap<>();

                for (Player killer : Bukkit.getOnlinePlayers()) {
                    if (!gm.isKiller(killer) || gm.isDead(killer))
                        continue;
                    UUID killerId = killer.getUniqueId();

                    for (UUID survivorId : gm.getSurvivorUUIDs()) {
                        Player survivor = Bukkit.getPlayer(survivorId);
                        if (survivor == null || !survivor.isOnline() || gm.isDead(survivor) || gm.hasEscaped(survivor))
                            continue;
                        if (survivor.getGameMode() == GameMode.SPECTATOR)
                            continue;

                        PlayerState state = plugin.getStateManager().getState(survivor);
                        if (state == PlayerState.HOOKED || state == PlayerState.DOWNED
                                || state == PlayerState.CARRIED) {
                            continue; // No chase if incapacitated
                        }

                        if (canSee(killer, survivor)) {
                            currentlySeen.put(survivorId, killerId);
                        }
                    }
                }

                // Process ongoing chases
                for (UUID survivorId : new java.util.HashSet<>(activeChases.keySet())) {
                    UUID killerId = activeChases.get(survivorId);
                    Player survivor = Bukkit.getPlayer(survivorId);
                    Player killer = Bukkit.getPlayer(killerId);

                    if (survivor == null || !survivor.isOnline() || killer == null || !killer.isOnline()
                            || gm.isDead(survivor) || gm.hasEscaped(survivor)
                            || plugin.getStateManager().getState(survivor) != PlayerStateManager.PlayerState.HEALTHY
                                    && plugin.getStateManager()
                                            .getState(survivor) != PlayerStateManager.PlayerState.INJURED) {
                        endChase(survivorId, killerId);
                        continue;
                    }

                    if (currentlySeen.containsKey(survivorId) && currentlySeen.get(survivorId).equals(killerId)) {
                        // Killer still sees survivor
                        chaseLostCounters.put(survivorId, 0); // Reset lost counter
                    } else {
                        // Killer lost sight
                        int lostTicks = chaseLostCounters.getOrDefault(survivorId, 0) + 10; // Task runs every 10 ticks
                        chaseLostCounters.put(survivorId, lostTicks);

                        // Check distance as well. If they are too far, end chase faster?
                        // Or if distance > CHASE_END_DISTANCE, end immediately
                        if (lostTicks >= MAX_LOST_TICKS || !survivor.getWorld().equals(killer.getWorld())
                                || survivor.getLocation()
                                        .distanceSquared(
                                                killer.getLocation()) > CHASE_END_DISTANCE * CHASE_END_DISTANCE) {
                            endChase(survivorId, killerId);
                            continue;
                        }
                    }

                    // Handle Music Loop
                    int currentTicks = chaseTimeCounters.getOrDefault(survivorId, 0) + 10;
                    chaseTimeCounters.put(survivorId, currentTicks);
                    int maxTicks = plugin.getConfig().getInt("chase-music-length-seconds", 175) * 20;
                    if (currentTicks >= maxTicks) {
                        chaseTimeCounters.put(survivorId, 0);
                        // Re-play music
                        survivor.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);

                        // Paper API: playSound attached to entity, so it naturally fades out over
                        // distance!
                        survivor.playSound(killer, org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS, 10.0f,
                                1.0f);

                        // Stop and replay for killer
                        killer.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);
                        killer.playSound(killer.getLocation(), org.bukkit.Sound.MUSIC_DISC_CREATOR,
                                SoundCategory.RECORDS, 1.0f, 1.0f);
                    }
                }

                // Start new chases
                for (Map.Entry<UUID, UUID> entry : currentlySeen.entrySet()) {
                    UUID survivorId = entry.getKey();
                    UUID killerId = entry.getValue();

                    if (!activeChases.containsKey(survivorId)) {
                        startChase(survivorId, killerId);
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // Run every half second
    }

    public void stopTask() {
        if (chaseTask != null) {
            chaseTask.cancel();
            chaseTask = null;
        }

        // Stop all chases
        for (UUID survId : new java.util.HashSet<>(activeChases.keySet())) {
            endChase(survId, activeChases.get(survId));
        }
        activeChases.clear();
        chaseLostCounters.clear();
        chaseTimeCounters.clear();
    }

    private boolean canSee(Player killer, Player survivor) {
        if (!killer.getWorld().equals(survivor.getWorld()))
            return false;

        double distanceSq = killer.getLocation().distanceSquared(survivor.getLocation());
        if (distanceSq > CHASE_START_DISTANCE * CHASE_START_DISTANCE)
            return false;

        // Fix for "x not finite" Exception - prevents division by zero in normalize()
        // if players are inside each other
        if (distanceSq < 0.0001)
            return true;

        Vector toSurvivor = survivor.getLocation().toVector().subtract(killer.getLocation().toVector()).normalize();
        Vector killerDirection = killer.getLocation().getDirection();

        // Check FOV
        if (killerDirection.dot(toSurvivor) < FOV_ANGLE) {
            return false;
        }

        // Check Line of Sight (RayTrace)
        Location eyeLoc = killer.getEyeLocation();
        Vector dir = toSurvivor;
        double dist = Math.sqrt(distanceSq);

        org.bukkit.util.RayTraceResult result = killer.getWorld().rayTraceBlocks(eyeLoc, dir, dist,
                org.bukkit.FluidCollisionMode.NEVER, true);

        // If it hit a block before reaching the survivor, LOS is blocked
        if (result != null && result.getHitBlock() != null) {
            return false;
        }

        return true;
    }

    private void startChase(UUID survivorId, UUID killerId) {
        activeChases.put(survivorId, killerId);
        chaseLostCounters.put(survivorId, 0);

        Player survivor = Bukkit.getPlayer(survivorId);
        Player killer = Bukkit.getPlayer(killerId);

        if (survivor != null && killer != null) {
            // Stop any playing sounds first
            survivor.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);
            killer.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);

            // Odtwarzanie muzyki
            // Z użyciem nowej mechaniki PaperAPI doczepiając dźwięk do Killera (muzyka
            // oddala się i przybliża)
            survivor.playSound(killer, org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS, 10.0f, 1.0f);

            killer.playSound(killer.getLocation(), org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS, 1.0f,
                    1.0f);
        }
    }

    private void endChase(UUID survivorId, UUID killerId) {
        activeChases.remove(survivorId);
        chaseLostCounters.remove(survivorId);

        Player survivor = Bukkit.getPlayer(survivorId);
        Player killer = Bukkit.getPlayer(killerId);

        if (survivor != null) {
            survivor.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);
        }
        if (killer != null) {
            // Only stop killer's music if they are not chasing anyone else
            if (!activeChases.containsValue(killerId)) {
                killer.stopSound(org.bukkit.Sound.MUSIC_DISC_CREATOR, SoundCategory.RECORDS);
            }
        }
    }

    public void onPlayerIncapacitated(Player player) {
        if (activeChases.containsKey(player.getUniqueId())) {
            UUID killerId = activeChases.get(player.getUniqueId());
            endChase(player.getUniqueId(), killerId);
        }
    }
}
