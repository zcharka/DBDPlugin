package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class HealCommand implements CommandExecutor {
    private final DBDPlugin plugin;
    private final PlayerStateManager stateManager;

    public HealCommand(DBDPlugin plugin, PlayerStateManager stateManager) {
        this.plugin = plugin;
        this.stateManager = stateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage(DBDPlugin.getMsg("clbrakuprawnie"));
            return true;
        }

        Player target = null;
        if (args.length == 0) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage("Musisz podac gracza.");
            }
        } else {
            if (args[0].equalsIgnoreCase("all")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    stateManager.heal(p);
                    p.sendMessage("§aZostałeś wyleczony przez administratora!");
                }
                sender.sendMessage("§aWyleczono wszystkich graczy.");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
        }

        if (target != null) {
            stateManager.heal(target);
            target.sendMessage("§aZostałeś wyleczony przez administratora!");
            sender.sendMessage("§aWyleczono gracza " + target.getName() + ".");
        } else {
            sender.sendMessage("§cGracz o podanej nazwie nie jest online.");
        }

        return true;
    }
}
