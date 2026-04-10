package pl.dbd.state;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <h1>API DLA INNYCH AI ORAZ SKRYPTÓW (skript-reflect)</h1>
 * Klasa <code>PlayerStateManager</code> zarządza stanami zdrowia graczy
 * (Ocalałych) w grze.
 * <br>
 * <br>
 * <h2>1. Jak poprawnie zmieniać stan zdrowia gracza?</h2>
 * Aby prawidłowo zmienić stan zdrowia gracza (np. na INJURED, HEALTHY), należy
 * ZAWSZE używać tej instancji:
 * 
 * <pre>{@code
 * DBDPlugin plugin = DBDPlugin.getInstance(); // Upewnij się, że masz instancję pluginu DBD
 * PlayerStateManager psm = plugin.getStateManager();
 * psm.setState(player, PlayerStateManager.PlayerState.INJURED);
 * }</pre>
 * 
 * Możliwe stany dla drugiego argumentu (<code>PlayerState</code>):
 * <ul>
 * <li><code>HEALTHY</code> - W pełni zdrowy, domyślna prędkość biegania.</li>
 * <li><code>INJURED</code> - Ranny (Zostawia czerwoną ścieżkę z cząsteczek
 * Redstone, pojękuje, powolniejsze leczenie do full).</li>
 * <li><code>DOWNED</code> - Powalony (Leży, czołga się, ma pasek powrotu do
 * życia - recovery).</li>
 * <li><code>HOOKED</code> - Powieszony na haku.</li>
 * <li><code>CARRIED</code> - Podniesiony/niesiony na ramieniu mordercy.</li>
 * <li><code>DEAD</code> / <code>IN_LOCKER</code> - Oczywiste (martwy lub ukryty
 * wewnątrz szafki).</li>
 * </ul>
 * <br>
 * <h2>2. Jak zrobić leżenie (DOWNED)?</h2>
 * Stan leżenia (animacja czołgania się poniżej bloku) to w kodzie
 * <code>PlayerState.DOWNED</code>.
 * Aby poprawnie powalić Gracza na ziemię, <b>najlepiej używać wyższego wrappera
 * z GameManager</b>:
 * 
 * <pre>{@code
 * plugin.getGameManager().setDownedState(player);
 * }</pre>
 * 
 * Powyższa metoda automatycznie obsłuży zmianę stanu, ukarze prędkością
 * czołgania i co najważniejsze
 * – wyświetli komunikat na Chacie (np. <i>"Gracz X został powalony!"</i>).
 * <br>
 * <br>
 * <b>Z poziomu samego PlayerStateManager:</b> (nie ogłosi globalnej
 * informacji):
 * 
 * <pre>{@code
 * plugin.getStateManager().setDowned(player.getUniqueId());
 * }</pre>
 * 
 * To wywoła za Ciebie niezbędne <code>setState(DOWNED)</code> oraz
 * <code>applyDownedState(player)</code>, które nakłada przymus
 * ograniczeń i animacji czołgania. Nigdy nie przypisuj gołego parametru
 * <code>setState(..., DOWNED)</code>
 * samodzielnie, by uniknąć glitchów postaci (bo to nie wywołuje aktualizacji
 * wymiarów i prędkości hitboxa)!
 */
public class PlayerStateManager {
    private final DBDPlugin plugin;
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> crawlTasks = new HashMap<>();
    private final Map<UUID, org.bukkit.Location> lastBarrierLocs = new HashMap<>();

    public PlayerStateManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public enum PlayerState {
        HEALTHY, INJURED, DOWNED, HOOKED, CARRIED, DEAD, IN_LOCKER
    }

    public PlayerState getState(Player p) {
        return states.getOrDefault(p.getUniqueId(), PlayerState.HEALTHY);
    }

    public void setState(Player p, PlayerState state) {
        PlayerState current = getState(p);

        // W trakcie trwającej gry nie pozwalamy "wskrzesić" gracza
        // DEAD innym stanem niż DEAD. DEAD ma obowiązywać do końca meczu.
        if (current == PlayerState.DEAD && state != PlayerState.DEAD) {
            if (plugin.getGameManager() != null
                    && plugin.getGameManager().isInGame(p)
                    && plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.IN_GAME) {
                return;
            }
        }

        states.put(p.getUniqueId(), state);
        if (state == PlayerState.DOWNED || state == PlayerState.HOOKED || state == PlayerState.CARRIED
                || state == PlayerState.DEAD) {
            if (plugin.getChaseManager() != null) {
                plugin.getChaseManager().onPlayerIncapacitated(p);
            }
        }
    }

    public void handleHit(Player p, Player killer) {
        PlayerState current = getState(p);

        if (current == PlayerState.HEALTHY) {
            setState(p, PlayerState.INJURED);
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("czostaeZraniony"));
            killer.sendMessage("§cZraniłeś gracza: " + p.getName());

            // Boost mobilności po hicie dla ocalałego (jak sprint w DBD) - Speed 1 na 3
            // sekundy
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));

        } else if (current == PlayerState.INJURED) {
            setState(p, PlayerState.DOWNED);
            setDowned(p.getUniqueId());
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("czostaepowalony"));
            killer.sendMessage("§4Powaliłeś gracza: " + p.getName());
        }
    }

    // --- METODY WYMAGANE PRZEZ INNE PLIKI (NAPRAWA BŁĘDÓW) ---

    public boolean isDowned(Player p) {
        return getState(p) == PlayerState.DOWNED;
    }

    public boolean isInjured(Player p) {
        return getState(p) == PlayerState.INJURED;
    }

    public boolean isCarried(Player p) {
        return getState(p) == PlayerState.CARRIED;
    }

    public boolean isHooked(Player p) {
        return getState(p) == PlayerState.HOOKED;
    }

    public void setDowned(UUID uuid) {
        Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null) {
            setState(p, PlayerState.DOWNED);
            applyDownedState(p);
        }
    }

    public void setInjured(UUID uuid) {
        Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null)
            setState(p, PlayerState.INJURED);
    }

    public void applyDownedState(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 3, false, false));

        if (crawlTasks.containsKey(p.getUniqueId())) {
            crawlTasks.get(p.getUniqueId()).cancel();
            crawlTasks.remove(p.getUniqueId());
        }
        org.bukkit.scheduler.BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (getState(p) != PlayerState.DOWNED || !p.isOnline()) {
                    p.setPose(org.bukkit.entity.Pose.STANDING, false);
                    if (lastBarrierLocs.containsKey(p.getUniqueId())) {
                        org.bukkit.Location oldLoc = lastBarrierLocs.remove(p.getUniqueId());
                        p.sendBlockChange(oldLoc, oldLoc.getBlock().getBlockData());
                    }
                    this.cancel();
                    crawlTasks.remove(p.getUniqueId());
                    return;
                }
                p.setPose(org.bukkit.entity.Pose.SWIMMING, false);

                org.bukkit.Location currentBarrierLoc = p.getLocation().clone().add(0, 1.5, 0).getBlock().getLocation();
                org.bukkit.Location oldLoc = lastBarrierLocs.get(p.getUniqueId());

                if (oldLoc != null && !oldLoc.equals(currentBarrierLoc)) {
                    p.sendBlockChange(oldLoc, oldLoc.getBlock().getBlockData());
                }

                if (currentBarrierLoc.getBlock().getType().isAir()) {
                    p.sendBlockChange(currentBarrierLoc, org.bukkit.Material.BARRIER.createBlockData());
                    lastBarrierLocs.put(p.getUniqueId(), currentBarrierLoc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        crawlTasks.put(p.getUniqueId(), task);
    }

    public void heal(Player p) {
        // Jeśli gracz jest oznaczony jako DEAD w trwającym meczu,
        // nie pozwalamy go uleczyć aż do zakończenia gry.
        if (getState(p) == PlayerState.DEAD) {
            if (plugin.getGameManager() != null
                    && plugin.getGameManager().isInGame(p)
                    && plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.IN_GAME) {
                return;
            }
        }

        setState(p, PlayerState.HEALTHY);
        p.setPose(org.bukkit.entity.Pose.STANDING, false);
        p.setSwimming(false);
        p.setWalkSpeed(0.2F);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.removePotionEffect(PotionEffectType.JUMP_BOOST);
        if (crawlTasks.containsKey(p.getUniqueId())) {
            crawlTasks.get(p.getUniqueId()).cancel();
            crawlTasks.remove(p.getUniqueId());
        }
        if (lastBarrierLocs.containsKey(p.getUniqueId())) {
            org.bukkit.Location oldLoc = lastBarrierLocs.remove(p.getUniqueId());
            p.sendBlockChange(oldLoc, oldLoc.getBlock().getBlockData());
        }
    }

    public void cleanup(UUID uuid) {
        states.remove(uuid);
        if (crawlTasks.containsKey(uuid)) {
            crawlTasks.get(uuid).cancel();
            crawlTasks.remove(uuid);
        }
        lastBarrierLocs.remove(uuid);
    }
}