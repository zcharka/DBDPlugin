package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.game.GameManager;

public class GameCommand implements CommandExecutor {
   private final GameManager gameManager;

   public GameCommand(GameManager gameManager) {
      this.gameManager = gameManager;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!(sender instanceof Player player)) return true;
      
      if (!player.hasPermission("dbc.admin")) {
          player.sendMessage("§cBrak uprawnień.");
          return true;
      }
      
      if (args.length == 0) {
         player.sendMessage("§8§m-------§r §6§lDBD GAME §8§m-------");
         player.sendMessage("§e/game setlobby §7- Ustawia spawn lobby");
         player.sendMessage("§e/game setkilledspawn §7- Ustawia spawn dla ZABITYCH");
         player.sendMessage("§e/game setescapedspawn §7- Ustawia spawn dla UCIECKINIERÓW");
         player.sendMessage("§e/game start §7- Wymusza start gry");
         player.sendMessage("§e/game stop §7- Przerywa grę");
         player.sendMessage("§e/game info §7- Status gry");
         return true;
      }
      
      try {
         switch(args[0].toLowerCase()) {
            case "setlobby":
               this.gameManager.setLobbySpawn(player.getLocation());
               player.sendMessage("§aPomyślnie ustawiono spawn lobby!");
               break;
               
            case "setkilledspawn":
               this.gameManager.setKilledSpawn(player.getLocation());
               player.sendMessage("§cPomyślnie ustawiono spawn dla ZABITYCH!");
               break;
               
            case "setescapedspawn":
               this.gameManager.setEscapedSpawn(player.getLocation());
               player.sendMessage("§bPomyślnie ustawiono spawn dla UCIECKINIERÓW!");
               break;

            case "start":
               this.gameManager.forceStart();
               player.sendMessage("§6Wymuszono start gry!");
               break;

            case "stop":
               if (this.gameManager.getGameState() == GameManager.GameState.LOBBY || 
                   this.gameManager.getGameState() == GameManager.GameState.ENDED) {
                   player.sendMessage("§cGra aktualnie nie trwa!");
                   return true;
               }
               
               this.gameManager.endGame(GameManager.GameEndReason.ALL_DEAD);
               player.sendMessage("§cZatrzymano grę.");
               break;

            case "info":
               player.sendMessage("§7Stan: §e" + this.gameManager.getGameState().name());
               player.sendMessage("§7Mapa: §e" + this.gameManager.getCurrentMap());
               player.sendMessage("§7Lobby Spawn: §e" + (this.gameManager.getLobbySpawn() != null ? "Ustawiony" : "Brak"));
               break;

            default:
               player.sendMessage("§cNieznana komenda.");
         }
      } catch (Exception e) {
         player.sendMessage("§cWystąpił błąd wewnętrzny! Sprawdź konsolę.");
         e.printStackTrace();
      }
      return true;
   }
}