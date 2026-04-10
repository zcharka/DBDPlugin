package pl.dbd.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

public class ReadyCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public ReadyCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;
        GameManager gm = plugin.getGameManager();

        if (plugin.getGameBanManager() != null && plugin.getGameBanManager().isBanned(p.getUniqueId())) {
            p.sendMessage("§c§lJESTEŚ ZBANOWANY Z ROZGRYWKI! 🚫");
            p.sendMessage("§cPowód: §7" + plugin.getGameBanManager().getBanReason(p.getUniqueId()));
            p.sendMessage(
                    "§cCzas do końca kary: §e" + plugin.getGameBanManager().getFormattedRemainingTime(p.getUniqueId()));
            return true;
        }

        // 1. Sprawdź czy lobby jest ustawione
        if (gm.getLobbySpawn() == null) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("clobbygryniejestusta"));
            return true;
        }

        // 3. Logika dołączania
        if (gm.getReadyPlayers().contains(p.getUniqueId())) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("ejujestewkolejce"));
            return true;
        }

        // Dodajemy do lobby lub kolejki
        gm.joinLobby(p);

        return true;
    }
}