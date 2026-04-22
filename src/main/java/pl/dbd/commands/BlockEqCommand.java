package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.dbd.DBDPlugin;
import pl.dbd.equipment.EquipmentManager;

public class BlockEqCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public BlockEqCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage("§cBrak uprawnień.");
            return true;
        }

        EquipmentManager eqMngr = plugin.getEquipmentManager();
        if (eqMngr == null)
            return true;

        boolean newState = !eqMngr.isEquipmentBlocked();
        eqMngr.setEquipmentBlocked(newState);

        if (newState) {
            sender.sendMessage("§aZablokowano wydawanie perków i przedmiotów w kolejnych meczach!");
            org.bukkit.Bukkit.broadcastMessage("§c§l[!] §7Wydawanie przedmiotów i perków zostało zablokowane!");
        } else {
            sender.sendMessage("§aOdblokowano wydawanie perków i przedmiotów!");
            org.bukkit.Bukkit.broadcastMessage("§a§l[!] §7Wydawanie przedmiotów i perków zostało przywrócone!");
        }
        return true;
    }
}
