package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class ShopCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public ShopCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        
        if (plugin.getShopManager() == null) {
            p.sendMessage("§c[DEBUG] ShopManager jest null!");
            return true;
        }
        
        plugin.getShopManager().openShop(p);
        return true;
    }
}