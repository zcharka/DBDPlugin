package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class DebugCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public DebugCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) return true;
        if (args.length < 2) return false;

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) return true;

        if (args[0].equalsIgnoreCase("injured")) {
            plugin.getStateManager().setState(target, PlayerStateManager.PlayerState.INJURED);
            sender.sendMessage("Ustawiono stan: INJURED");
        } else if (args[0].equalsIgnoreCase("healthy")) {
            plugin.getStateManager().heal(target);
            sender.sendMessage("Uleczono gracza.");
        } else if (args[0].equalsIgnoreCase("downed")) {
            plugin.getStateManager().setDowned(target.getUniqueId());
            sender.sendMessage("Powalono gracza.");
        }
        return true;
    }
}