package pl.dbd.window;

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
         sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctakomendamoebyuytat"));
         return true;
      }
      Player p = (Player) sender;
      if (!p.hasPermission("dbd.admin")) {
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbrakuprawnie"));
         return true;
      }
      if (args.length == 0) {
         this.sendHelp(p);
         return true;
      }

      String sub = args[0].toLowerCase();
      Location loc = p.getLocation().getBlock().getLocation();

      if (sub.equals("create") || sub.equals("toggle") || sub.equals("add")) {
         this.manager.toggle(loc);
         if (this.manager.isWindow(loc)) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("aloknoutworzone"));
            p.sendMessage("§7Lokalizacja: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
         } else {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cloknousunite"));
         }
      } else if (sub.equals("remove")) {
         if (args.length >= 2) {
            try {
               int id = Integer.parseInt(args[1]) - 1;
               if (this.manager.removeWindow(id)) {
                  p.sendMessage("§aZ pomyślnością usunięto okno ID: " + (id + 1));
               } else {
                  p.sendMessage("§cNie znaleziono okna o ID: " + (id + 1));
               }
            } catch (NumberFormatException e) {
               p.sendMessage("§cPodaj prawidłowe ID okna (np. /window remove 1). Użyj /window list");
            }
         } else {
            p.sendMessage("§cPodaj ID okna do usunięcia. Użyj /window list");
         }
      } else if (sub.equals("list")) {
         int count = this.manager.getWindowCount();
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("elokna"));
         p.sendMessage("§7Całkowita liczba: §e" + count);
         if (count > 0) {
            int i = 1;
            for (Location window : this.manager.getAllWindows()) {
               String wName = window.getWorld() != null ? window.getWorld().getName() : "brak";
               p.sendMessage("§e" + i + ". §7" + window.getBlockX() + ", " + window.getBlockY() + ", "
                     + window.getBlockZ() + " §8(" + wName + ")");
               i++;
            }
         }
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("msg2"));
      } else if (sub.equals("reset")) {
         this.manager.resetAllWindows();
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("alwszystkieoknazrese"));
      } else if (sub.equals("info")) {
         boolean isWindow = this.manager.isWindow(loc);
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("elinfookna"));
         p.sendMessage("§7Twoja lokalizacja: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
         p.sendMessage("§7Czy to okno: " + (isWindow ? "§a✓ TAK" : "§c✗ NIE"));
         p.sendMessage("§7Całkowita liczba okien: §e" + this.manager.getWindowCount());
         p.sendMessage(pl.dbd.DBDPlugin.getMsg("msg3"));
      } else {
         this.sendHelp(p);
      }
      return true;
   }

   private void sendHelp(Player p) {
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("elwindow"));
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("ewindowcreate7tworzy"));
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("ewindowlist7listawsz"));
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("ewindowreset7usuwaws"));
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("ewindowinfo7informac"));
      p.sendMessage(pl.dbd.DBDPlugin.getMsg("msg4"));
   }
}
