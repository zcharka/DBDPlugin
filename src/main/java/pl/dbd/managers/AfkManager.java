package pl.dbd.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.dbd.DBDPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkManager implements Listener {
    private final DBDPlugin plugin;
    private final Map<UUID, Long> lastMove = new HashMap<>();

    public AfkManager(DBDPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startCheckTask();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        lastMove.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    private void startCheckTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (lastMove.containsKey(p.getUniqueId())) {
                    if (now - lastMove.get(p.getUniqueId()) > 1000 * 60 * 5) { // 5 minut
                        // Logika AFK (np. kick lub info)
                    }
                }
            }
        }, 200L, 200L);
    }
    
    public void onGameStart() { lastMove.clear(); }
    public void onGameEnd() { lastMove.clear(); }
}