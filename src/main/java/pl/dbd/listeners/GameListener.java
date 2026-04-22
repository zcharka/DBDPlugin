package pl.dbd.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.dbd.DBDPlugin;

public class GameListener implements Listener {
    private final DBDPlugin plugin;

    public GameListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    // --- MECHANIKA KRWI ---
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        
        // Jeśli gracz jest ranny lub powalony
        if (plugin.getStateManager().isInjured(p) || plugin.getStateManager().isDowned(p)) {
            if (e.getFrom().distance(e.getTo()) > 0.1 && Math.random() < 0.25) {
                // POPRAWKA: Używamy Particle.BLOCK dla nowszych wersji (zamiast BLOCK_CRACK)
                try {
                    p.getWorld().spawnParticle(
                        Particle.BLOCK, 
                        p.getLocation(), 
                        4, 
                        0.2, 0.0, 0.2, 
                        Material.REDSTONE_BLOCK.createBlockData()
                    );
                } catch (IllegalArgumentException ex) {
                    // Fallback dla starszych wersji 1.12 i niżej (jeśli używasz)
                    // p.getWorld().spawnParticle(Particle.valueOf("BLOCK_CRACK"), ...);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        e.setCancelled(true);
        e.setFoodLevel(20);
    }
}