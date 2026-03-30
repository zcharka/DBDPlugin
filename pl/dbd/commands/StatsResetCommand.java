package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class StatsResetCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public StatsResetCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("nopermission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /" + label + " [gracz]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cGracz o podanym nicku nie jest online!");
            return true;
        }

        plugin.getPlayerDataManager().resetGames(target.getUniqueId());
        sender.sendMessage("§aPomyślnie zresetowano statystyki gier gracza: §e" + target.getName());
        return true;
    }
}
