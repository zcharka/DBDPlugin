package pl.dbd.punishment;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.dbd.DBDPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class GameBanManager {
    private final DBDPlugin plugin;
    private final File banFile;
    private FileConfiguration banData;

    public GameBanManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.banFile = new File(plugin.getDataFolder(), "game-bans.yml");
        loadBans();
    }

    public void loadBans() {
        if (!banFile.exists()) {
            try {
                banFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        banData = YamlConfiguration.loadConfiguration(banFile);
    }

    private void saveBans() {
        try {
            banData.save(banFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void banPlayer(UUID uuid, long durationMs, String reason) {
        long endTimestamp = System.currentTimeMillis() + durationMs;
        banData.set(uuid.toString() + ".banned_until", endTimestamp);
        banData.set(uuid.toString() + ".reason", reason);
        saveBans();
    }

    public void unbanPlayer(UUID uuid) {
        banData.set(uuid.toString(), null);
        saveBans();
    }

    public boolean isBanned(UUID uuid) {
        if (!banData.contains(uuid.toString() + ".banned_until")) {
            return false;
        }
        long endTimestamp = banData.getLong(uuid.toString() + ".banned_until", 0);
        if (System.currentTimeMillis() > endTimestamp) {
            unbanPlayer(uuid);
            return false;
        }
        return true;
    }

    public String getBanReason(UUID uuid) {
        return banData.getString(uuid.toString() + ".reason", "Brak podanego powodu.");
    }

    public String getFormattedRemainingTime(UUID uuid) {
        long endTimestamp = banData.getLong(uuid.toString() + ".banned_until", 0);
        long remaining = endTimestamp - System.currentTimeMillis();
        if (remaining <= 0)
            return "Czas minął";

        long days = remaining / (1000 * 60 * 60 * 24);
        long hours = (remaining / (1000 * 60 * 60)) % 24;
        long mins = (remaining / (1000 * 60)) % 60;
        long secs = (remaining / 1000) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (mins > 0)
            sb.append(mins).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
