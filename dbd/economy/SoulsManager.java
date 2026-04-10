package pl.dbd.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import java.util.*;
import java.util.stream.Collectors;

public class SoulsManager {
    private final DBDPlugin plugin;

    public SoulsManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    // ADAPTER: Naprawia błędy "Player cannot be converted to UUID"
    public int getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public int getBalance(UUID uuid) {
        return plugin.getConfig().getInt("players." + uuid + ".souls", 0);
    }

    public void add(Player player, int amount) {
        int current = getBalance(player.getUniqueId());
        setBalance(player.getUniqueId(), current + amount);
        player.sendMessage("§b§lDUSZE §8▸ §aOtrzymałeś §e" + amount + " §7Dusz Nexusa!");
    }

    public boolean take(Player player, int amount) {
        int current = getBalance(player.getUniqueId());
        if (current >= amount) {
            setBalance(player.getUniqueId(), current - amount);
            return true;
        }
        return false;
    }

    public void setBalance(Player player, int amount) {
        setBalance(player.getUniqueId(), amount);
    }

    public void setBalance(UUID uuid, int amount) {
        plugin.getConfig().set("players." + uuid + ".souls", Math.max(0, amount));
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        if (op.getName() != null) {
            plugin.getConfig().set("players." + uuid + ".last_name", op.getName());
        }
        plugin.saveConfig();
    }
    
    public Map<String, Integer> getTopPlayers(int limit) {
        if (plugin.getConfig().getConfigurationSection("players") == null) return new HashMap<>();
        Map<String, Integer> all = new HashMap<>();
        for (String key : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
            int souls = plugin.getConfig().getInt("players." + key + ".souls", 0);
            String name = plugin.getConfig().getString("players." + key + ".last_name", "Nieznany");
            all.put(name, souls);
        }
        return all.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}