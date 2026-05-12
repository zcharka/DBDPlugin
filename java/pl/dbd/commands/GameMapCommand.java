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
         sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctakomendamoebyuytat"));
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
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cuyciegamemapcreaten"));
                  return true;
               }

               String mapName = args[1];
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("elkrok127ustawspawns"));
               player.sendMessage("§7Wpisz: §e/gamemap setsurvspawn " + mapName);
               break;
            case 1:
               if (args.length < 2) {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cuyciegamemapsetsurv"));
                  return true;
               }

               this.gameManager.tempSurvSpawn = player.getLocation();
               this.gameManager.tempMapName = args[1];
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("alspawnsurvivorwzapi"));
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("elkrok227ustawspawnk"));
               player.sendMessage("§7Wpisz: §e/gamemap setkillspawn " + args[1]);
               break;
            case 2:
               if (args.length < 2) {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cuyciegamemapsetkill"));
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
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cuyciegamemapremoven"));
                  return true;
               }

               this.gameManager.removeMap(args[1]);
               player.sendMessage("§cUsunięto mapę: " + args[1]);
               break;
            case 5:
               Set<String> mapNames = this.gameManager.getMaps();
               if (mapNames.isEmpty()) {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakmap"));
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("7stwrzmapegamemapcre"));
                  break;
               } else {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("eldostpnemapy"));
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
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("elgamemap"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("7gamemapcreatenazwa8"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("7gamemapsetsurvspawn"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("7gamemapsetkillspawn"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("7gamemapremovenazwa8"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("7gamemaplist87listam"));
      player.sendMessage("");
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("elprzykad"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("71egamemapcreatemoja"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("727stagdziemajspawns"));
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("737stagdziemaspawnki"));
   }

   private String formatLocation(Location loc) {
      int var10000 = loc.getBlockX();
      return var10000 + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
   }
}
