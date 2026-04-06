package pl.dbd.penalty;

import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

/**
 * Manages penalties for players who disconnect during a game.
 * Extend this class with your desired penalty logic.
 */
public class QuitPenaltyManager {

    private final DBDPlugin plugin;

    public QuitPenaltyManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a player quits during an active game.
     * @param player the player who quit
     */
    public void applyPenalty(Player player) {
        int penalty = plugin.getConfig().getInt("quit-penalty.souls", 50);
        if (penalty > 0 && plugin.getSoulsManager() != null) {
            // Zmiana z removeSouls na take (zgodnie z NexusSoulsManager)
            plugin.getSoulsManager().take(player, penalty);
            plugin.getLogger().info("[QuitPenalty] " + player.getName()
                    + " stracił " + penalty + " dusz za wyjście z gry.");
        }
    }
}
