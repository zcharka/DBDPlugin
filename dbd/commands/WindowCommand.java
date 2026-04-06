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
         sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctakomendamoebyuytat"));
         return true;
      } else {
         Player player = (Player)sender;
         if (!player.hasPermission("dbd.admin")) {
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakpermisji"));
            return true;
         } else if (args.length == 0) {
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("elwindowcommand"));
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("7windowcreatetworzyo"));
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("7windowremoveusuwaok"));
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("7windowlistlistaokie"));
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
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("aoknoutworzone"));
               break;
            case 1:
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("coknousunite"));
               break;
            case 2:
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("elistaokien0"));
               break;
            default:
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("cnieznanasubkomenda"));
            }

            return true;
         }
      }
   }
}
