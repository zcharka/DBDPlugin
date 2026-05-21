package pl.dbd.game;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * [SKRIPT-REFLECT] INSTRUKCJA UŻYCIA
 * 
 * Ta klasa zarządza Terror Radiusem (Płyta 11). Dźwięk jest podpinany pod killera
 * z odpowiednią głośnością (1.875f), co daje dokładnie 30 kratek zasięgu (klient Minecrafta
 * sam ścisza i pogłaśnia dźwięk w zależności od odległości gracza od killera).
 * Dźwięk NIE JEST wysyłany do ocalałych, gdy wyjdą poza 30 kratek lub wejdą w pościg.
 * 
 * --- UŻYCIE W SKRIPT-REFLECT ---
 * Jeżeli chcesz ingerować w Terror Radius (np. zablokować go przez pewien czas na zrzucenie agresji):
 * 
 * 1. Pobierz instancję:
 *    set {_manager} to plugin "DBDPlugin".getTerrorRadiusManager()
 * 
 * 2. Zablokuj (wycisz i zatrzymaj) terror radius konkretnemu graczowi:
 *    {_manager}.setIgnored(player, true)
 * 
 * 3. Odblokuj terror radius (wznowi się samoczynnie jeśli jest w zasięgu):
 *    {_manager}.setIgnored(player, false)
 */
public class TerrorRadiusManager {

    private final DBDPlugin plugin;
    private BukkitRunnable task;
    private final Map<UUID, Long> lastPlayTime = new HashMap<>();
    private final Set<UUID> ignoredPlayers = new HashSet<>();

    public TerrorRadiusManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getGameManager().getGameState() != GameManager.GameState.IN_GAME) {
                    cancel();
                    return;
                }

                long now = System.currentTimeMillis();

                for (UUID survId : plugin.getGameManager().getSurvivors()) {
                    Player surv = plugin.getServer().getPlayer(survId);
                    if (surv == null || plugin.getGameManager().isDead(surv) || plugin.getGameManager().hasEscaped(surv)) continue;

                    // Sprawdzamy czy ocalały jest zignorowany lub w pościgu
                    boolean ignored = ignoredPlayers.contains(survId);
                    boolean inChase = false;
                    if (plugin.getChaseManager() != null) {
                        inChase = plugin.getChaseManager().isInChase(surv);
                    }

                    // Znajdź najbliższego killera
                    Player nearestKiller = null;
                    double minDistSqr = Double.MAX_VALUE;
                    for (UUID killerId : plugin.getGameManager().getKillers()) {
                        Player killer = plugin.getServer().getPlayer(killerId);
                        if (killer != null && killer.getWorld().equals(surv.getWorld())) {
                            double dist = killer.getLocation().distanceSquared(surv.getLocation());
                            if (dist < minDistSqr) {
                                minDistSqr = dist;
                                nearestKiller = killer;
                            }
                        }
                    }

                    // 30 kratek = 900 dystans do kwadratu
                    boolean inRadius = nearestKiller != null && minDistSqr <= 900.0;

                    // Jeżeli zignorowany, w pościgu, lub poza promieniem -> odcinamy dźwięk
                    if (ignored || inChase || !inRadius) {
                        if (lastPlayTime.containsKey(survId)) {
                            surv.stopSound(Sound.MUSIC_DISC_11, SoundCategory.RECORDS);
                            lastPlayTime.remove(survId);
                        }
                        continue;
                    }

                    // Gracz jest w promieniu (i nie jest ignorowany / nie jest w pościgu)
                    long lastPlayed = lastPlayTime.getOrDefault(survId, 0L);
                    
                    // Płyta 11 trwa ok. 71s. Odświeżamy ją po 70.5s żeby zapętlić na spokojnie
                    if (now - lastPlayed >= 70500) {
                        surv.stopSound(Sound.MUSIC_DISC_11, SoundCategory.RECORDS);
                        // głośność 1.875 = równo 30 kratek zasięgu (klient tnie dźwięk równo za 30. kratką)
                        surv.playSound(nearestKiller, Sound.MUSIC_DISC_11, SoundCategory.RECORDS, 1.875f, 1.0f);
                        lastPlayTime.put(survId, now);
                    }
                }
            }
        };
        // Sprawdzaj co 10 ticków (pół sekundy), aby błyskawicznie reagować na wejście w zasięg/pościg
        task.runTaskTimer(plugin, 0L, 10L); 
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID survId : plugin.getGameManager().getSurvivors()) {
            Player surv = plugin.getServer().getPlayer(survId);
            if (surv != null) {
                surv.stopSound(Sound.MUSIC_DISC_11, SoundCategory.RECORDS);
            }
        }
        lastPlayTime.clear();
        ignoredPlayers.clear();
    }

    /**
     * Zablokuj/odblokuj terror radius dla danego gracza (przydatne do skript-reflect).
     */
    public void setIgnored(Player player, boolean ignored) {
        if (ignored) {
            ignoredPlayers.add(player.getUniqueId());
            player.stopSound(Sound.MUSIC_DISC_11, SoundCategory.RECORDS);
            lastPlayTime.remove(player.getUniqueId());
        } else {
            ignoredPlayers.remove(player.getUniqueId());
        }
    }
}
