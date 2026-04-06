package pl.dbd.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.locker.Locker;
import pl.dbd.locker.LockerManager;

public class LockerCommand implements CommandExecutor, TabCompleter {
   private final DBDPlugin plugin;
   private final LockerManager lockerManager;

   public LockerCommand(DBDPlugin plugin) {
      this.plugin = plugin;
      this.lockerManager = plugin.getLockerManager();
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctakomendamoebyuytat"));
         return true;
      }

      if (!player.hasPermission("dbd.admin")) {
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("cniemaszuprawniedote"));
         return true;
      }

      if (args.length == 0) {
         sendHelp(player);
         return true;
      }

      Block targetBlock = player.getTargetBlockExact(10);

      switch (args[0].toLowerCase()) {
         case "create": {
            if (args.length < 2) {
               player.sendMessage("§cUżycie: /locker create <mapa>");
               return true;
            }
            if (targetBlock == null || targetBlock.getType() != Material.IRON_DOOR) {
               player.sendMessage("§cMusisz patrzeć na żelazne drzwi!");
               return true;
            }
            String mapName = args[1].toLowerCase();
            lockerManager.createLocker(targetBlock.getLocation(), mapName);
            player.sendMessage("§aSzafa utworzona na mapie: §e" + mapName);
            player.sendMessage("§7Lokalizacja: " + targetBlock.getX() + ", " + targetBlock.getY() + ", "
                  + targetBlock.getZ());
            break;
         }

         case "remove":
         case "delete": {
            if (targetBlock == null || targetBlock.getType() != Material.IRON_DOOR) {
               player.sendMessage("§cMusisz patrzeć na żelazne drzwi!");
               return true;
            }
            if (lockerManager.removeLocker(targetBlock.getLocation())) {
               player.sendMessage("§aSzafa usunięta.");
            } else {
               player.sendMessage("§cTo nie jest zarejestrowana szafka.");
            }
            break;
         }

         case "list": {
            List<Locker> lockers = lockerManager.getAllLockers();
            player.sendMessage("§e§lAktywne szafy: §7(" + lockers.size() + ")");
            for (Locker l : lockers) {
               String occupied = l.hasPlayerInside() ? "§c[ZAJĘTA]" : "§a[WOLNA]";
               player.sendMessage("§7- " + occupied + " §e"
                     + l.getDoorLocation().getBlockX() + ", "
                     + l.getDoorLocation().getBlockY() + ", "
                     + l.getDoorLocation().getBlockZ());
            }
            break;
         }

         case "load": {
            if (args.length < 2) {
               player.sendMessage("§cUżycie: /locker load <mapa>");
               return true;
            }
            String mapName = args[1].toLowerCase();
            lockerManager.activateMap(mapName);
            player.sendMessage("§aZaładowano szafy dla mapy: §e" + mapName);
            break;
         }

         case "reset": {
            lockerManager.resetAllLockers();
            player.sendMessage("§aWszystkie szafy zresetowane.");
            break;
         }

         default:
            sendHelp(player);
      }
      return true;
   }

   private void sendHelp(Player player) {
      player.sendMessage("§e§lKomendy szaf:");
      player.sendMessage("§a/locker create <mapa> §7- Tworzy szafę na patrzonej lokalizacji (żelazne drzwi)");
      player.sendMessage("§a/locker remove §7- Usuwa szafę z patrzonej lokalizacji");
      player.sendMessage("§a/locker list §7- Lista aktywnych szaf");
      player.sendMessage("§a/locker load <mapa> §7- Ładuje szafy z mapy");
      player.sendMessage("§a/locker reset §7- Resetuje wszystkie szafy");
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return Arrays.asList("create", "remove", "list", "load", "reset");
      }
      if (args.length == 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("load"))) {
         return new ArrayList<>(lockerManager.getMapNames());
      }
      return new ArrayList<>();
   }
}