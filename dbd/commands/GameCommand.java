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
      if (!(sender instanceof Player player))
         return true;

      if (!player.hasPermission("dbd.admin")) {
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakuprawnie"));
         return true;
      }

      if (args.length == 0) {
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("8mr6ldbdgame8m"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egamesetlobby7ustawi"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egamesetkilledspawn7"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egamesetescapedspawn"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egamestart7wymuszast"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egamestop7przerywagr"));
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("egameinfo7statusgry"));
         return true;
      }

      try {
         switch (args[0].toLowerCase()) {
            case "setlobby":
               this.gameManager.setLobbySpawn(player.getLocation());
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("apomylnieustawionosp"));
               break;

            case "setkilledspawn":
               this.gameManager.setKilledSpawn(player.getLocation());
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("cpomylnieustawionosp"));
               break;

            case "setescapedspawn":
               this.gameManager.setEscapedSpawn(player.getLocation());
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("bpomylnieustawionosp"));
               break;

            case "sethookedspawn":
               this.gameManager.setHookedSpawn(player.getLocation());
               player.sendMessage("§eSpawn dla powieszonych na haku (hooked) został ustawiony poprawnie!");
               break;

            case "start":
               if (this.gameManager.getGameState() == GameManager.GameState.LOBBY
                     || this.gameManager.getGameState() == GameManager.GameState.ENDED) {
                  this.gameManager.forceStart();
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("6wymuszonostartgry"));
               } else {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cgrajutrwa"));
               }
               break;

            case "stop":
               if (this.gameManager.getGameState() == GameManager.GameState.LOBBY ||
                     this.gameManager.getGameState() == GameManager.GameState.ENDED) {
                  player.sendMessage(pl.dbd.DBDPlugin.getMsg("cgraaktualnienietrwa"));
                  return true;
               }

               this.gameManager.endGame(GameManager.GameEndReason.ALL_DEAD);
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("czatrzymanogr"));
               break;

            case "info":
               player.sendMessage("§7Stan: §e" + this.gameManager.getGameState().name());
               player.sendMessage("§7Mapa: §e" + this.gameManager.getCurrentMap());
               player.sendMessage(
                     "§7Lobby Spawn: §e" + (this.gameManager.getLobbySpawn() != null ? "Ustawiony" : "Brak"));
               break;

            case "remove":
               if (args.length < 2) {
                  player.sendMessage("§cUżycie: /game remove <nazwa_mapy>");
                  return true;
               }
               String mapName = args[1];
               if (!this.gameManager.getMaps().contains(mapName)) {
                  player.sendMessage("§cNie ma takiej mapy: " + mapName);
                  return true;
               }
               this.gameManager.removeMap(mapName);
               player.sendMessage("§aUsunięto mapę: §e" + mapName);
               break;

            default:
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("cnieznanakomenda"));
         }
      } catch (Exception e) {
         player.sendMessage(pl.dbd.DBDPlugin.getMsg("cwystpibdwewntrznysp"));
         e.printStackTrace();
      }
      return true;
   }
}