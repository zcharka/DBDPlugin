package pl.dbd.display;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class HealthDisplayManager {
   private final DBDPlugin plugin;
   private final PlayerStateManager stateManager;

   public HealthDisplayManager(DBDPlugin plugin, PlayerStateManager stateManager) {
      this.plugin = plugin;
      this.stateManager = stateManager;
      this.startUpdateTask();
   }

   private void startUpdateTask() {
      (new BukkitRunnable() {
         public void run() {
            Iterator var1 = Bukkit.getOnlinePlayers().iterator();

            while(var1.hasNext()) {
               Player player = (Player)var1.next();
               HealthDisplayManager.this.updateDisplay(player);
            }

         }
      }).runTaskTimer(this.plugin, 0L, 20L);
   }

   private void updateDisplay(Player player) {
      if (player.hasMetadata("NPC")) {
         player.setCustomNameVisible(false);
      } else {
         PlayerStateManager.PlayerState state = this.stateManager.getState(player);
         String color;
         String stateText;
         switch(state) {
         case HEALTHY:
            color = "§a";
            stateText = "ZDROWY";
            break;
         case INJURED:
            color = "§e";
            stateText = "RANNY";
            break;
         case DOWNED:
            color = "§c";
            stateText = "LEŻĄCY";
            break;
         case CARRIED:
            color = "§4";
            stateText = "NIESIONY";
            break;
         case HOOKED:
            color = "§4";
            stateText = "NA HAKU";
            break;
         case DEAD:
            color = "§8";
            stateText = "MARTWY";
            break;
         default:
            color = "§7";
            stateText = state.toString();
         }

         player.setCustomName(color + player.getName() + " §8[" + color + stateText + "§8]");
         player.setCustomNameVisible(true);
      }

   }
}
