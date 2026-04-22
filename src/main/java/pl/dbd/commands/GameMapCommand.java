package pl.dbd.commands;

import java.util.Iterator;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.game.GameManager;

public class GameMapCommand implements CommandExecutor {
   private final GameManager gameManager;

   public GameMapCommand(GameManager gameManager) {
      this.gameManager = gameManager;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage("§cTa komenda może być użyta tylko przez gracza!");
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 0) {
            this.sendHelp(player);
            return true;
         } else {
            String var6 = args[0].toLowerCase();
            byte var7 = -1;
            switch(var6.hashCode()) {
            case -2009451333:
               if (var6.equals("setkillspawn")) {
                  var7 = 2;
               }
               break;
            case -1352294148:
               if (var6.equals("create")) {
                  var7 = 0;
               }
               break;
            case -1335458389:
               if (var6.equals("delete")) {
                  var7 = 4;
               }
               break;
            case -934610812:
               if (var6.equals("remove")) {
                  var7 = 3;
               }
               break;
            case 3322014:
               if (var6.equals("list")) {
                  var7 = 5;
               }
               break;
            case 1466015987:
               if (var6.equals("setsurvspawn")) {
                  var7 = 1;
               }
            }

            switch(var7) {
            case 0:
               if (args.length < 2) {
                  player.sendMessage("§cUżycie: /gamemap create <nazwa>");
                  return true;
               }

               String mapName = args[1];
               player.sendMessage("§e§lKrok 1/2: §7Ustaw spawn SURVIVORÓW");
               player.sendMessage("§7Wpisz: §e/gamemap setsurvspawn " + mapName);
               break;
            case 1:
               if (args.length < 2) {
                  player.sendMessage("§cUżycie: /gamemap setsurvspawn <nazwa>");
                  return true;
               }

               this.gameManager.tempSurvSpawn = player.getLocation();
               this.gameManager.tempMapName = args[1];
               player.sendMessage("§a§lSpawn survivorów zapisany!");
               player.sendMessage("§e§lKrok 2/2: §7Ustaw spawn KILLERA");
               player.sendMessage("§7Wpisz: §e/gamemap setkillspawn " + args[1]);
               break;
            case 2:
               if (args.length < 2) {
                  player.sendMessage("§cUżycie: /gamemap setkillspawn <nazwa>");
                  return true;
               }

               if (this.gameManager.tempMapName != null && this.gameManager.tempMapName.equals(args[1])) {
                  this.gameManager.addMap(args[1], this.gameManager.tempSurvSpawn, player.getLocation());
                  player.sendMessage("§a§l✓ Mapa utworzona: §e" + args[1]);
                  player.sendMessage("§7Survivor spawn: §a" + this.formatLocation(this.gameManager.tempSurvSpawn));
                  String var10001 = this.formatLocation(player.getLocation());
                  player.sendMessage("§7Killer spawn: §c" + var10001);
                  this.gameManager.tempSurvSpawn = null;
                  this.gameManager.tempMapName = null;
                  break;
               }

               player.sendMessage("§cPierw ustaw spawn survivorów: §e/gamemap setsurvspawn " + args[1]);
               return true;
            case 3:
            case 4:
               if (args.length < 2) {
                  player.sendMessage("§cUżycie: /gamemap remove <nazwa>");
                  return true;
               }

               this.gameManager.removeMap(args[1]);
               player.sendMessage("§cUsunięto mapę: " + args[1]);
               break;
            case 5:
               Set<String> mapNames = this.gameManager.getMaps();
               if (mapNames.isEmpty()) {
                  player.sendMessage("§cBrak map!");
                  player.sendMessage("§7Stwórz mapę: §e/gamemap create <nazwa>");
                  break;
               } else {
                  player.sendMessage("§e§l=== Dostępne mapy ===");
                  Iterator var10 = mapNames.iterator();

                  while(var10.hasNext()) {
                     String name = (String)var10.next();
                     player.sendMessage("§7- §e" + name);
                  }

                  return true;
               }
            default:
               this.sendHelp(player);
            }

            return true;
         }
      }
   }

   private void sendHelp(Player player) {
      player.sendMessage("§e§l=== Game Map ===");
      player.sendMessage("§7/gamemap create <nazwa> §8- §7Rozpocznij tworzenie mapy");
      player.sendMessage("§7/gamemap setsurvspawn <nazwa> §8- §7Ustaw spawn survivorów");
      player.sendMessage("§7/gamemap setkillspawn <nazwa> §8- §7Ustaw spawn killera");
      player.sendMessage("§7/gamemap remove <nazwa> §8- §7Usuń mapę");
      player.sendMessage("§7/gamemap list §8- §7Lista map");
      player.sendMessage("");
      player.sendMessage("§e§lPrzykład:");
      player.sendMessage("§71. §e/gamemap create MojaArena");
      player.sendMessage("§72. §7Stań gdzie mają spawn survivors → §e/gamemap setsurvspawn MojaArena");
      player.sendMessage("§73. §7Stań gdzie ma spawn killer → §e/gamemap setkillspawn MojaArena");
   }

   private String formatLocation(Location loc) {
      int var10000 = loc.getBlockX();
      return var10000 + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
   }
}
