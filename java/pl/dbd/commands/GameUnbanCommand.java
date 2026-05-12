package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.dbd.DBDPlugin;

import java.util.UUID;

public class GameUnbanCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public GameUnbanCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage("§cBrak uprawnień!");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /gameunban <nick>");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage("§cGracz nie znaleziony!");
            return true;
        }

        UUID uuid = target.getUniqueId();
        if (plugin.getGameBanManager() != null) {
            if (!plugin.getGameBanManager().isBanned(uuid)) {
                sender.sendMessage("§cGracz " + target.getName() + " nie jest zbanowany z rozgrywki.");
                return true;
            }
            plugin.getGameBanManager().unbanPlayer(uuid);
            sender.sendMessage("§aPomyślnie odbanowano gracza §e" + target.getName() + " §az rozgrywki!");
        } else {
            sender.sendMessage("§cGameBanManager jest niezainicjalizowany!");
        }

        return true;
    }
}
