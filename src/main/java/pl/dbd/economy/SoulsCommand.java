package pl.dbd.economy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import java.util.Map;

public class SoulsCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public SoulsCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                long balance = plugin.getSoulsManager().getBalance(p);
                p.sendMessage("§7Twoje Dusze: §e" + balance);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            sender.sendMessage("§8§m-------§r §6TOP 10 DUSZ §8§m-------");
            int i = 1;
            // Poprawiona pętla iterująca po mapie
            for (Map.Entry<String, Integer> entry : plugin.getSoulsManager().getTopPlayers(10).entrySet()) {
                sender.sendMessage("§e" + i + ". §7" + entry.getKey() + " - §6" + entry.getValue());
                i++;
            }
            return true;
        }

        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        if (args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cGracz offline.");
                return true;
            }
            
            try {
                long amount = Long.parseLong(args[2]);
                
                if (args[0].equalsIgnoreCase("add")) {
                    plugin.getSoulsManager().add(target, amount);
                    sender.sendMessage("§aDodano.");
                } else if (args[0].equalsIgnoreCase("take")) {
                    plugin.getSoulsManager().take(target, amount);
                    sender.sendMessage("§aZabrano.");
                } else if (args[0].equalsIgnoreCase("set")) {
                    plugin.getSoulsManager().setBalance(target, amount);
                    sender.sendMessage("§aUstawiono.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cPodaj poprawną liczbę.");
            }
        }
        return true;
    }
}