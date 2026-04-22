package pl.dbd.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.dbd.DBDPlugin;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {
    private final DBDPlugin plugin;
    private final File folder;

    public PlayerDataManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists())
            folder.mkdirs();
    }

    private File getFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    public int getSouls(UUID uuid) {
        File f = getFile(uuid);
        if (!f.exists())
            return 0;
        return YamlConfiguration.loadConfiguration(f).getInt("souls", 0);
    }

    public void setSouls(UUID uuid, int amount) {
        File f = getFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("souls", amount);
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Obsługa wygranych/przegranych
    public int getWins(UUID uuid) {
        File f = getFile(uuid);
        return YamlConfiguration.loadConfiguration(f).getInt("wins", 0);
    }

    public int getLosses(UUID uuid) {
        File f = getFile(uuid);
        return YamlConfiguration.loadConfiguration(f).getInt("losses", 0);
    }

    public int getGamesPlayed(UUID uuid) {
        return getWins(uuid) + getLosses(uuid);
    }

    public void addWin(UUID uuid) {
        File f = getFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("wins", cfg.getInt("wins", 0) + 1);
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLoss(UUID uuid) {
        File f = getFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("losses", cfg.getInt("losses", 0) + 1);
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetGames(UUID uuid) {
        File f = getFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("wins", 0);
        cfg.set("losses", 0);
        try {
            cfg.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public java.util.Map<UUID, Integer> getTopPlayersByWins(int limit) {
        return getTopList("wins", limit);
    }

    public java.util.Map<UUID, Integer> getTopPlayersByLosses(int limit) {
        return getTopList("losses", limit);
    }

    public java.util.Map<UUID, Integer> getTopPlayersByGames(int limit) {
        java.util.Map<UUID, Integer> map = new java.util.HashMap<>();
        if (!folder.exists() || folder.listFiles() == null)
            return map;

        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".yml")) {
                try {
                    UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                    org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration
                            .loadConfiguration(f);
                    int wins = cfg.getInt("wins", 0);
                    int losses = cfg.getInt("losses", 0);
                    map.put(uuid, wins + losses);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return sortMap(map, limit);
    }

    private java.util.Map<UUID, Integer> getTopList(String key, int limit) {
        java.util.Map<UUID, Integer> map = new java.util.HashMap<>();
        if (!folder.exists() || folder.listFiles() == null)
            return map;

        for (File f : folder.listFiles()) {
            if (f.getName().endsWith(".yml")) {
                try {
                    UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                    org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration
                            .loadConfiguration(f);
                    int val = cfg.getInt(key, 0);
                    map.put(uuid, val);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return sortMap(map, limit);
    }

    private java.util.Map<UUID, Integer> sortMap(java.util.Map<UUID, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(java.util.Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        java.util.Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new));
    }
}