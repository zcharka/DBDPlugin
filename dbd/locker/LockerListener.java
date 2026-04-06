package pl.dbd.locker;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import pl.dbd.DBDPlugin;

/**
 * Listener szafkowy — iron door interakcja (zamiast Skriptu)
 * Survivor: PPM na żelazne drzwi → wchodzi do szafy (jeśli zarejestrowana)
 * Killer: PPM na żelazne drzwi → wyciąga survivora z szafy (jeśli jest w
 * środku)
 * Survivor w szafie: kucanie (shift) → wychodzi ze szafy
 */
public class LockerListener implements Listener {
    private final DBDPlugin plugin;

    public LockerListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = e.getClickedBlock();
        if (block == null || block.getType() != Material.IRON_DOOR)
            return;

        Player p = e.getPlayer();
        LockerManager mgr = plugin.getLockerManager();
        if (mgr == null)
            return;

        Locker locker = mgr.getLockerAt(block.getLocation());
        if (locker == null)
            return; // To nie jest zarejestrowana szafka

        e.setCancelled(true); // Blokuj otwieranie żelaznych drzwi

        // === KILLER ===
        if (plugin.getGameManager().isKiller(p)) {
            if (locker.hasPlayerInside()) {
                locker.grabPlayer(p, plugin);
            } else {
                // Pusta szafka — dźwięk otwierania
                block.getLocation().getWorld().playSound(block.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.2f);
                p.sendMessage("§7Szafka jest pusta.");
            }
            return;
        }

        // === SURVIVOR ===
        if (plugin.getGameManager().isSurvivor(p)) {
            // Jeśli gracz jest już w szafie - wychodzi
            Locker insideLocker = mgr.getLockerForPlayer(p.getUniqueId());
            if (insideLocker != null) {
                insideLocker.exitLocker(p, plugin);
                return;
            }

            // Jeśli szafka jest zajęta
            if (locker.hasPlayerInside()) {
                p.sendMessage("§cTa szafka jest już zajęta!");
                return;
            }

            // Sprawdź dystans
            if (p.getLocation().distance(block.getLocation()) > 3.5) {
                p.sendMessage("§cJesteś za daleko od szafki!");
                return;
            }

            // Wchodzi do szafy
            locker.enterLocker(p, plugin);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking())
            return;
        Player p = e.getPlayer();
        LockerManager mgr = plugin.getLockerManager();
        if (mgr == null)
            return;

        // Jeśli gracz jest w szafie i kucnął — wychodzi
        Locker locker = mgr.getLockerForPlayer(p.getUniqueId());
        if (locker != null) {
            locker.exitLocker(p, plugin);
        }
    }

    // FIX: Ukrywanie graczy w szafach dla osób, które dopiero weszły na serwer
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player joiningPlayer = e.getPlayer();
        LockerManager mgr = plugin.getLockerManager();
        if (mgr == null) return;

        for (Locker locker : mgr.getAllLockers()) {
            if (locker.hasPlayerInside()) {
                Player hiddenPlayer = Bukkit.getPlayer(locker.getPlayerInside());
                // Ważne: Nie ukrywaj gracza przed samym sobą (jeśli to reconnect)
                if (hiddenPlayer != null && !hiddenPlayer.equals(joiningPlayer)) {
                    // Ukryj gracza w szafie przed nowym graczem
                    joiningPlayer.hidePlayer(plugin, hiddenPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LockerManager mgr = plugin.getLockerManager();
        if (mgr == null) return;

        // Jeśli gracz wychodzi będąc w szafie, wyrzuć go z niej (logicznie)
        Locker l = mgr.getLockerForPlayer(e.getPlayer().getUniqueId());
        if (l != null) {
            l.exitLocker(e.getPlayer(), plugin);
        }
    }
}
