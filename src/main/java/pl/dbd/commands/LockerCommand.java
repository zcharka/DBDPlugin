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
         sender.sendMessage("§cTa komenda może być użyta tylko przez gracza!");
         return true;
      }
      
      if (!player.hasPermission("dbd.admin")) {
         player.sendMessage("§cNie masz uprawnień do tej komendy!");
         return true;
      }
      
      if (args.length == 0) {
         this.sendHelp(player);
         return true;
      }
      
      Block targetBlock = player.getTargetBlockExact(10);

      switch(args[0].toLowerCase()) {
         case "add":
         case "create":
            if (targetBlock == null || targetBlock.getType().isAir()) {
               player.sendMessage("§cMusisz patrzeć na blok!");
               return true;
            }
            if (!this.isDoor(targetBlock.getType())) {
               player.sendMessage("§cMusisz patrzeć na drzwi! (drewniane, żelazne, itp.)");
               return true;
            }
            this.lockerManager.createLocker(targetBlock.getLocation());
            player.sendMessage("§a§lSzafa utworzona na drzwiach na które patrzysz!");
            player.sendMessage("§7Lokalizacja: " + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ());
            break;
            
         case "remove":
         case "delete":
            if (targetBlock == null || targetBlock.getType().isAir()) {
               player.sendMessage("§cMusisz patrzeć na blok!");
               return true;
            }
            Locker locker = this.lockerManager.getLockerAt(targetBlock.getLocation());
            if (locker != null) {
               this.lockerManager.removeLocker(targetBlock.getLocation());
               player.sendMessage("§a§lSzafa usunięta!");
            } else {
               player.sendMessage("§cNie ma szafy na tym bloku!");
            }
            break;
            
         case "list":
            List<Locker> lockers = this.lockerManager.getAllLockers();
            player.sendMessage("§e§lLista szaf: §7(" + lockers.size() + ")");
            for(Locker l : lockers) {
               String occupied = l.hasPlayerInside() ? "§c[ZAJĘTA]" : "§a[WOLNA]";
               player.sendMessage("§7- " + occupied + " §e" + l.getDoorLocation().getBlockX() + ", " + l.getDoorLocation().getBlockY() + ", " + l.getDoorLocation().getBlockZ() + " §7(kierunek: " + l.getFacing() + ")");
            }
            break;
            
         case "reset":
            this.lockerManager.resetAllLockers();
            player.sendMessage("§a§lWszystkie szafy zresetowane!");
            break;
            
         default:
            this.sendHelp(player);
      }
      return true;
   }

   private void sendHelp(Player player) {
      player.sendMessage("§e§l=== Komendy Szaf ===");
      player.sendMessage("§a/locker create §7- Tworzy szafę na drzwiach na które patrzysz");
      player.sendMessage("§a/locker remove §7- Usuwa szafę na drzwiach na które patrzysz");
      player.sendMessage("§a/locker list §7- Lista wszystkich szaf");
      player.sendMessage("§a/locker reset §7- Resetuje wszystkie szafy");
   }

   private boolean isDoor(Material material) {
      return material.name().endsWith("_DOOR");
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      return args.length == 1 ? Arrays.asList("create", "remove", "list", "reset") : new ArrayList<>();
   }
}