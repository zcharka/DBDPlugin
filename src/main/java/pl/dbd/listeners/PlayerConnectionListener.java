package pl.dbd.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
        if (plugin.getGameManager().getLobbySpawn() != null) {
            e.getPlayer().teleport(plugin.getGameManager().getLobbySpawn());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        // Obsługa wyjścia podczas gry (Combat Log / Penalty)
        if (plugin.getGameManager().isInGame(e.getPlayer())) {
            plugin.getGameManager().removePlayer(e.getPlayer().getUniqueId());
        }
    }
}