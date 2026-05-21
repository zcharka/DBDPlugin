package pl.dbd.game;

import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.dbd.DBDPlugin;

public class ReadyBlockListener implements Listener {
   private final DBDPlugin plugin;
   private final GameManager gameManager;

   public ReadyBlockListener(DBDPlugin plugin, GameManager gameManager) {
      this.plugin = plugin;
      this.gameManager = gameManager;
   }

   @EventHandler
   public void onReadyBlock(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
         Player player = event.getPlayer();
         Location loc = event.getClickedBlock().getLocation();
         List<Location> readyBlocks = this.gameManager.getReadyBlocks();
         boolean isReadyBlock = false;
         Iterator var6 = readyBlocks.iterator();

         while(var6.hasNext()) {
            Location readyLoc = (Location)var6.next();
            if (readyLoc.equals(loc)) {
               isReadyBlock = true;
               break;
            }
         }

         if (isReadyBlock) {
            event.setCancelled(true);
            if (this.gameManager.getReadyPlayers().contains(player.getUniqueId())) {
               this.gameManager.leaveLobby(player);
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("copucielobby"));
            } else {
               this.gameManager.joinLobby(player);
               player.sendMessage(pl.dbd.DBDPlugin.getMsg("adoczyedolobby"));
            }
         }
      }

   }
}
