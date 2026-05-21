package pl.dbd.movement;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ScratchMarkManager {
    private final DBDPlugin plugin;
    private final List<Mark> marks = new ArrayList<>();

    public ScratchMarkManager(DBDPlugin plugin) {
        this.plugin = plugin;
        startTask();
    }

    private static class Mark {
        Location loc;
        long expiryTime;

        Mark(Location loc, long expiryTime) {
            this.loc = loc;
            this.expiryTime = expiryTime;
        }
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                pl.dbd.game.GameManager gm = plugin.getGameManager();

                if (gm == null)
                    return;

                // 1. Zbieranie śladów (sprintujący ocalali)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (gm.isInGame(p) && !gm.getKillers().contains(p.getUniqueId()) && p.isSprinting()
                            && !p.hasMetadata("NPC")) {
                        PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
                        if (state != PlayerStateManager.PlayerState.DEAD &&
                                state != PlayerStateManager.PlayerState.HOOKED) {

                            // Tworzymy 2 "rysowane" ślady (lekkie rozrzucenie na boki i góra-dół) by
                            // przypominały zadrapania
                            for (int i = 0; i < 2; i++) {
                                Location mLoc = p.getLocation().clone().add(
                                        (Math.random() - 0.5) * 1.2,
                                        0.1,
                                        (Math.random() - 0.5) * 1.2);
                                marks.add(new Mark(mLoc, now + 10000L)); // Znaki znikają po równych 10s
                            }
                        }
                    }
                }

                // 2. Szukanie The Killera/ów
                List<Player> killers = new ArrayList<>();
                for (UUID kId : gm.getKillers()) {
                    Player kp = Bukkit.getPlayer(kId);
                    if (kp != null && kp.isOnline()) {
                        killers.add(kp);
                    }
                }

                // 3. Rysowanie i usuwanie
                Iterator<Mark> it = marks.iterator();
                while (it.hasNext()) {
                    Mark m = it.next();
                    if (now > m.expiryTime) {
                        it.remove(); // Usuń wygasnięte ślady
                    } else if (!killers.isEmpty()) {
                        long age = now - (m.expiryTime - 10000L);
                        // Im starszy ślad, tym nieco rzadziej będzie rysowany
                        if (age > 7000L && Math.random() < 0.3) {
                            continue; // Pozwala śladowi po 7s zanikać wizualnie na oczach The Killera
                        }

                        for (Player kp : killers) {
                            if (kp.getWorld().equals(m.loc.getWorld())) {
                                // Spawnowanie cząsteczek WYŁĄCZNIE DLA PODMIOTU KILLERA na ziemi
                                kp.spawnParticle(Particle.SOUL_FIRE_FLAME, m.loc, 1, 0.05, 0.0, 0.05, 0.0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // 5 ticków to 0.25 sekundy - bardzo optymalne!
    }

    public void cleanup() {
        marks.clear();
    }
}
