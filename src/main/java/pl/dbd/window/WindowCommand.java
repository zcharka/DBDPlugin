package pl.dbd.window;

import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WindowCommand implements CommandExecutor {
   private final WindowManager manager;

   public WindowCommand(WindowManager manager) {
      this.manager = manager;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cTa komenda może być użyta tylko przez gracza!");
         return true;
      } else {
         Player p = (Player)sender;
         if (!p.hasPermission("dbd.admin")) {
            p.sendMessage("§c§lBrak uprawnień!");
            return true;
         } else if (args.length == 0) {
            this.sendHelp(p);
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
            case -868304044:
               if (var6.equals("toggle")) {
                  var7 = 2;
               }
               break;
            case 96417:
               if (var6.equals("add")) {
                  var7 = 1;
               }
               break;
            case 3237038:
               if (var6.equals("info")) {
                  var7 = 5;
               }
               break;
            case 3322014:
               if (var6.equals("list")) {
                  var7 = 3;
               }
               break;
            case 108404047:
               if (var6.equals("reset")) {
                  var7 = 4;
               }
            }

            int var10001;
            switch(var7) {
            case 0:
            case 1:
            case 2:
               Location loc = p.getLocation().getBlock().getLocation();
               this.manager.toggle(loc);
               if (this.manager.isWindow(loc)) {
                  p.sendMessage("§a§lOkno utworzone!");
                  var10001 = loc.getBlockX();
                  p.sendMessage("§7Lokalizacja: " + var10001 + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
               } else {
                  p.sendMessage("§c§lOkno usunięte!");
               }
               break;
            case 3:
               int count = this.manager.getWindowCount();
               p.sendMessage("§e§l═══════ OKNA ═══════");
               p.sendMessage("§7Całkowita liczba: §e" + count);
               if (count > 0) {
                  int i = 1;

                  for(Iterator var14 = this.manager.getAllWindows().iterator(); var14.hasNext(); ++i) {
                     Location window = (Location)var14.next();
                     p.sendMessage("§e" + i + ". §7" + window.getBlockX() + ", " + window.getBlockY() + ", " + window.getBlockZ());
                  }
               }

               p.sendMessage("§e§l═══════════════════════");
               break;
            case 4:
               this.manager.resetAllWindows();
               p.sendMessage("§a§lWszystkie okna zresetowane!");
               break;
            case 5:
               Location currentLoc = p.getLocation().getBlock().getLocation();
               boolean isWindow = this.manager.isWindow(currentLoc);
               p.sendMessage("§e§l═══════ INFO OKNA ═══════");
               var10001 = currentLoc.getBlockX();
               p.sendMessage("§7Twoja lokalizacja: §e" + var10001 + ", " + currentLoc.getBlockY() + ", " + currentLoc.getBlockZ());
               p.sendMessage("§7Czy to okno: " + (isWindow ? "§a✓ TAK" : "§c✗ NIE"));
               p.sendMessage("§7Całkowita liczba okien: §e" + this.manager.getWindowCount());
               p.sendMessage("§e§l═════════════════════════");
               break;
            default:
               this.sendHelp(p);
            }

            return true;
         }
      }
   }

   private void sendHelp(Player p) {
      p.sendMessage("§e§l═══════ WINDOW ═══════");
      p.sendMessage("§e/window create §7- Tworzy okno w miejscu gdzie stoisz");
      p.sendMessage("§e/window list §7- Lista wszystkich okien");
      p.sendMessage("§e/window reset §7- Usuwa wszystkie okna");
      p.sendMessage("§e/window info §7- Informacje o oknie");
      p.sendMessage("§e§l══════════════════════");
   }
}
