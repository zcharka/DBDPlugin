package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class HealAllCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public HealAllCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage(DBDPlugin.getMsg("clbrakuprawnie"));
            return true;
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            plugin.getStateManager().heal(p);
            p.sendMessage("§aZostaliście wyleczeni przez administratora!");
        }
        sender.sendMessage("§aPomyślnie wyleczono wszystkich graczy.");
        return true;
    }
}
