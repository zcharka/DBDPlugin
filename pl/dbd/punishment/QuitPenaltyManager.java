package pl.dbd.punishment;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.dbd.DBDPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class QuitPenaltyManager implements Listener {
    private final DBDPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public QuitPenaltyManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "penalties.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try { config.save(file); } catch (IOException ignored) {}
    }

    public void handleQuit(Player p) {
        UUID uuid = p.getUniqueId();
        int warnings = config.getInt(uuid.toString() + ".warnings", 0) + 1;
        config.set(uuid.toString() + ".warnings", warnings);
        config.set(uuid.toString() + ".needs-warning", true);
        save();

        long banTime = 0;
        String reasonKey = "";

        int w10m = plugin.getConfig().getInt("quit-penalty.warnings-for-10m", 5);
        int w20m = plugin.getConfig().getInt("quit-penalty.warnings-for-20m", 10);
        int w2w = plugin.getConfig().getInt("quit-penalty.warnings-for-2w", 20);

        if (warnings >= w2w) {
            banTime = 14L * 24L * 60L * 60L * 1000L; // 14 dni
            reasonKey = "ban-reason-2w";
        } else if (warnings >= w20m) {
            banTime = 20L * 60L * 1000L; // 20 minut
            reasonKey = "ban-reason-20m";
        } else if (warnings >= w10m) {
            banTime = 10L * 60L * 1000L; // 10 minut
            reasonKey = "ban-reason-10m";
        }

        if (banTime > 0) {
            String reason = plugin.getConfig().getString("quit-penalty.messages." + reasonKey, "§cBan za opuszczanie gry.").replace("&", "§");
            Date unbanDate = new Date(System.currentTimeMillis() + banTime);
            Bukkit.getServer().getBanList(BanList.Type.NAME).addBan(p.getName(), reason, unbanDate, "System DBD");
            
            // Zerujemy konto po 2 tygodniowym banie
            if (warnings >= w2w) {
                config.set(uuid.toString() + ".warnings", 0);
                save();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        
        if (config.getBoolean(uuid.toString() + ".needs-warning", false)) {
            config.set(uuid.toString() + ".needs-warning", false);
            save();

            int warnings = config.getInt(uuid.toString() + ".warnings", 0);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getGameManager().getLobbySpawn() != null) {
                    p.teleport(plugin.getGameManager().getLobbySpawn());
                }
                
                String chat = plugin.getConfig().getString("quit-penalty.messages.warn-chat", "§cWyszedłeś z meczu! Ostrzeżenie: {warnings}").replace("{warnings}", String.valueOf(warnings)).replace("&", "§");
                String title = plugin.getConfig().getString("quit-penalty.messages.warn-title", "§c§lOSTRZEŻENIE").replace("&", "§");
                String sub = plugin.getConfig().getString("quit-penalty.messages.warn-subtitle", "§7Wyszedłeś podczas meczu!").replace("&", "§");
                
                p.sendMessage(chat);
                p.sendTitle(title, sub, 10, 70, 20);
            }, 20L); // Opóźnienie 1 sekunda po wejściu na serwer
        }
    }
}