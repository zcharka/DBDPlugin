package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class WindowCommand implements CommandExecutor {
   private final DBDPlugin plugin;

   public WindowCommand(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cTa komenda może być użyta tylko przez gracza!");
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("dbd.admin")) {
            player.sendMessage("§cBrak permisji!");
            return true;
         } else if (args.length == 0) {
            player.sendMessage("§e§l=== Window Command ===");
            player.sendMessage("§7/window create - Tworzy okno");
            player.sendMessage("§7/window remove - Usuwa okno");
            player.sendMessage("§7/window list - Lista okien");
            return true;
         } else {
            String var6 = args[0].toLowerCase();
            byte var7 = -1;
            switch(var6.hashCode()) {
            case -1352294148:
               if (var6.equals("create")) {
                  var7 = 0;
               }
               break;
            case -934610812:
               if (var6.equals("remove")) {
                  var7 = 1;
               }
               break;
            case 3322014:
               if (var6.equals("list")) {
                  var7 = 2;
               }
            }

            switch(var7) {
            case 0:
               player.sendMessage("§aOkno utworzone!");
               break;
            case 1:
               player.sendMessage("§cOkno usunięte!");
               break;
            case 2:
               player.sendMessage("§eLista okien: 0");
               break;
            default:
               player.sendMessage("§cNieznana subkomenda!");
            }

            return true;
         }
      }
   }
}
