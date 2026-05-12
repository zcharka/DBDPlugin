package pl.dbd.currency;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;
import pl.dbd.state.PlayerStateManager;

public class BloodPointsListener implements Listener {
   private final DBDPlugin plugin;
   private final BloodPointsManager bloodPointsManager;

   public BloodPointsListener(DBDPlugin plugin, BloodPointsManager bloodPointsManager) {
      this.plugin = plugin;
      this.bloodPointsManager = bloodPointsManager;
   }

   @EventHandler
   public void onKillerHit(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
         Player killer = (Player)event.getDamager();
         Player survivor = (Player)event.getEntity();
         GameManager gameManager = this.plugin.getGameManager();
         if (gameManager != null && gameManager.isKiller(killer) && gameManager.isSurvivor(survivor)) {
            PlayerStateManager stateManager = this.plugin.getStateManager();
            if (stateManager != null) {
               PlayerStateManager.PlayerState survivorState = stateManager.getState(survivor);
               if (survivorState == PlayerStateManager.PlayerState.HEALTHY) {
                  this.bloodPointsManager.grantSouls(killer, 1, "Zranienie survivora");
               } else if (survivorState == PlayerStateManager.PlayerState.INJURED) {
                  this.bloodPointsManager.grantSouls(killer, 2, "Powalenie survivora");
               }
            }
         }
      }

   }
}
