package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.shop.ShopAdminGUI;

public class ShopAdminCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    // Poprawiona nazwa konstruktora
    public ShopAdminCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("dbd.admin")) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7brakuprawnie"));
            return true;
        }

        // Otwieramy GUI edycji
        new ShopAdminGUI(plugin).open(p);
        return true;
    }
}