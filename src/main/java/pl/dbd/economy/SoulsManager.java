package pl.dbd.economy;

import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import java.util.*;
import java.util.stream.Collectors;

public class SoulsManager {
    private final DBDPlugin plugin;
    private final Map<UUID, Long> soulsCache = new HashMap<>();

    public SoulsManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public long getBalance(Player player) {
        return plugin.getPlayerDataManager().getSouls(player.getUniqueId());
    }

    public void add(Player player, long amount) {
        long current = getBalance(player);
        setBalance(player, current + amount);
        player.sendMessage("§aOtrzymano " + amount + " Dusz Nexusa!");
    }

    public boolean take(Player player, long amount) {
        long current = getBalance(player);
        if (current >= amount) {
            setBalance(player, current - amount);
            return true;
        }
        return false;
    }
    
    // Obsługa int dla kompatybilności
    public boolean take(Player player, int amount) {
        return take(player, (long) amount);
    }

    public void setBalance(Player player, long amount) {
        plugin.getPlayerDataManager().setSouls(player.getUniqueId(), (int) amount);
    }
    
    public Map<String, Integer> getTopPlayers(int limit) {
        // Prosta implementacja placeholderowa, w prawdziwym DB trzeba by sortować
        return new HashMap<>(); 
    }
}