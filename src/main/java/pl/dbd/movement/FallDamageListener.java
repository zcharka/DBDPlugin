package pl.dbd.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class FallDamageListener implements Listener {
   private final DBDPlugin plugin;

   public FallDamageListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onFall(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         final Player player = (Player)event.getEntity();
         if (event.getCause() == DamageCause.FALL) {
            if (this.plugin.getGameManager() != null) {
               if (this.plugin.getGameManager().isInGame(player)) {
                  event.setCancelled(true);
                  double fall = (double)player.getFallDistance();
                  int downedDist = this.plugin.getConfig().getInt("fall.downed-distance", 15);
                  int injuredDist = this.plugin.getConfig().getInt("fall.injured-distance", 12);
                  int slowDist = this.plugin.getConfig().getInt("fall.slowdown-distance", 8);
                  if (fall >= (double)downedDist) {
                     this.plugin.getStateManager().setState(player, PlayerStateManager.PlayerState.DOWNED);
                     this.plugin.getStateManager().applyDownedState(player);
                     player.sendTitle("§c§lLEŻY", "§7Spadłeś z " + downedDist + "+ bloków", 10, 50, 20);
                     this.keepSwimming(player);
                  } else if (fall >= (double)injuredDist) {
                     this.plugin.getStateManager().setInjured(player.getUniqueId());
                     player.sendTitle("§c§lRANNY", "§7Spadłeś z " + injuredDist + "+ bloków", 10, 50, 20);
                  } else if (fall >= (double)slowDist) {
                     player.sendActionBar("§eOtrzasasz się po upadku...");
                     final float orig = player.getWalkSpeed();
                     player.setWalkSpeed(0.05F);
                     (new BukkitRunnable() {
                        public void run() {
                           player.setWalkSpeed(orig);
                        }
                     }).runTaskLater(this.plugin, 20L);
                  }

               }
            }
         }
      }
   }

   private void keepSwimming(final Player player) {
      (new BukkitRunnable() {
         public void run() {
            if (FallDamageListener.this.plugin.getStateManager().getState(player) != PlayerStateManager.PlayerState.DOWNED) {
               player.setSwimming(false);
               this.cancel();
            } else {
               player.setSwimming(true);
            }

         }
      }).runTaskTimer(this.plugin, 0L, 1L);
   }
}
