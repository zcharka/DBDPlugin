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
        if (!folder.exists()) folder.mkdirs();
    }

    private File getFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }

    public int getSouls(UUID uuid) {
        File f = getFile(uuid);
        if (!f.exists()) return 0;
        return YamlConfiguration.loadConfiguration(f).getInt("souls", 0);
    }

    public void setSouls(UUID uuid, int amount) {
        File f = getFile(uuid);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
        cfg.set("souls", amount);
        try { cfg.save(f); } catch (IOException e) { e.printStackTrace(); }
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
}