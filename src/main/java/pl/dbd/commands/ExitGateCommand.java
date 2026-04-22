package pl.dbd.commands;

import java.util.Iterator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.exitgate.ExitGate;
import pl.dbd.exitgate.ExitGateManager;

public class ExitGateCommand implements CommandExecutor {
   private final ExitGateManager exitGateManager;

   public ExitGateCommand(DBDPlugin plugin) {
      this.exitGateManager = plugin.getExitGateManager();
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 0) {
            player.sendMessage("§e/exitgate create §7- Tworzy bramę wyjściową");
            player.sendMessage("§e/exitgate list §7- Lista bram");
            player.sendMessage("§e/exitgate reset §7- Resetuje wszystkie bramy");
            return true;
         } else {
            if (args[0].equalsIgnoreCase("create")) {
               this.exitGateManager.createExitGate(player.getLocation());
               player.sendMessage("§aUtworzono bramę wyjściową!");
            } else if (args[0].equalsIgnoreCase("list")) {
               player.sendMessage("§6Bramy wyjściowe: §f" + this.exitGateManager.getAllExitGates().size());
               player.sendMessage("§aOtwarte: §f" + this.exitGateManager.getOpenCount());
               int i = 1;

               for(Iterator var7 = this.exitGateManager.getAllExitGates().iterator(); var7.hasNext(); ++i) {
                  ExitGate gate = (ExitGate)var7.next();
                  player.sendMessage("§7#" + i + " - " + (gate.isOpened() ? "§a[OTWARTA]" : "§c[ZAMKNIĘTA]"));
               }
            } else if (args[0].equalsIgnoreCase("reset")) {
               this.exitGateManager.resetAllGates();
               player.sendMessage("§aZresetowano wszystkie bramy!");
            }

            return true;
         }
      }
   }
}
