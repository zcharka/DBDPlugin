package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class EquipmentCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public EquipmentCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (plugin.getEquipmentManager() == null) {
            p.sendMessage("§c[DEBUG] EquipmentManager jest null!");
            return true;
        }

        plugin.getEquipmentManager().openEquipment(p);
        return true;
    }
}