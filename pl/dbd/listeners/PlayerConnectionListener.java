package pl.dbd.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class PlayerConnectionListener implements Listener {
    private final DBDPlugin plugin;

    public PlayerConnectionListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        // Resetujemy stan gracza przy wejściu (bezpiecznik)
        plugin.getStateManager().heal(e.getPlayer());

        // Wyślij do lobby jeśli gra nie trwa
        org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld("lobby");
        if (lobbyWorld != null) {
            e.getPlayer().teleport(new org.bukkit.Location(lobbyWorld, 0.5, 1.0, -0.5));
        } else if (plugin.getGameManager().getLobbySpawn() != null) {
            e.getPlayer().teleport(plugin.getGameManager().getLobbySpawn());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        // Wyczyszczenie map w menedżerach (przeciwdziałanie Memory Leaks)
        plugin.getStateManager().cleanup(e.getPlayer().getUniqueId());
        if (plugin.getRecoveryManager() != null) {
            plugin.getRecoveryManager().resetProgress(e.getPlayer().getUniqueId());
        }

        // Obsługa wyjścia podczas gry (Combat Log / Penalty)
        if (plugin.getGameManager().isInGame(e.getPlayer())) {
            Player p = e.getPlayer();
            plugin.getGameManager().kickPlayer(p);
        } else {
            // Wyrzucenie z kolejki jeśli nie był w grze, a w lobby
            plugin.getGameManager().leaveLobby(e.getPlayer());
        }
    }
}