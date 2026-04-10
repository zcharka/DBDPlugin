package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.dbd.DBDPlugin;
import pl.dbd.managers.PlayerDataManager;
import pl.dbd.economy.SoulsManager;

import java.util.Map;
import java.util.UUID;

public class TopCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public TopCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.top")) {
            sender.sendMessage(DBDPlugin.getMsg("brakUprawnien"));
            return true;
        }

        PlayerDataManager pdm = plugin.getPlayerDataManager();
        SoulsManager sm = plugin.getSoulsManager();

        sender.sendMessage("\n§8-------------- §c§lTOPKA DUSZ NEXUSA §8----------------");
        Map<String, Integer> topSouls = sm.getTopPlayers(3);
        printTopStringMap(sender, topSouls);

        sender.sendMessage("\n§8-------------- §c§lTOPKA ROZEGRANYCH GIER §8------------");
        Map<UUID, Integer> topGames = pdm.getTopPlayersByGames(3);
        printTopUUIDMap(sender, topGames);

        sender.sendMessage("\n§8-------------- §c§lTOPKA WYGRANYCH GIER §8------------");
        Map<UUID, Integer> topWins = pdm.getTopPlayersByWins(3);
        printTopUUIDMap(sender, topWins);

        sender.sendMessage("\n§8-------------- §c§lTOPKA PRZEGRANYCH GIER §8------------");
        Map<UUID, Integer> topLosses = pdm.getTopPlayersByLosses(3);
        printTopUUIDMap(sender, topLosses);

        sender.sendMessage(""); // Pusta linia na koniec

        return true;
    }

    private void printTopStringMap(CommandSender sender, Map<String, Integer> map) {
        int i = 1;
        if (map.isEmpty()) {
            sender.sendMessage("§7Brak danych do wyświetlenia.");
        }
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            sender.sendMessage("§e" + i + ". miejsce: §a" + entry.getKey() + " §7(" + entry.getValue() + ")");
            i++;
        }
    }

    private void printTopUUIDMap(CommandSender sender, Map<UUID, Integer> map) {
        int i = 1;
        if (map.isEmpty()) {
            sender.sendMessage("§7Brak danych do wyświetlenia.");
        }
        for (Map.Entry<UUID, Integer> entry : map.entrySet()) {
            String name = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null)
                name = "Nieznany";
            sender.sendMessage("§e" + i + ". miejsce: §a" + name + " §7(" + entry.getValue() + ")");
            i++;
        }
    }
}
