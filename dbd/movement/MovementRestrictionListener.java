package pl.dbd.movement;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Pose;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class MovementRestrictionListener implements Listener {
   private final DBDPlugin plugin;
   private final Set<UUID> jumpAllowed = new HashSet<>();

   public MovementRestrictionListener(DBDPlugin plugin) {
      this.plugin = plugin;
      this.startSwimmingTask();
   }

   private void startSwimmingTask() {
      new BukkitRunnable() {
         public void run() {
            // Optymalizacja: użycie pętli foreach zamiast ręcznego iteratora
            for (Player p : Bukkit.getOnlinePlayers()) {
               if (MovementRestrictionListener.this.plugin.getStateManager() == null)
                  continue;

               PlayerStateManager.PlayerState state = MovementRestrictionListener.this.plugin.getStateManager()
                     .getState(p);
               if (state == PlayerStateManager.PlayerState.CARRIED) {
                  if (p.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                     p.removePotionEffect(PotionEffectType.JUMP_BOOST);
                  }
                  // Ustawiaj swimming tylko jeśli nie jest już ustawiony (mniej pakietów)
                  if (p.getPose() != Pose.SWIMMING) {
                     p.setPose(Pose.SWIMMING, true);
                  }
               } else if (state != PlayerStateManager.PlayerState.DOWNED && p.isSwimming()) {
                  p.setSwimming(false);
               }
            }
         }
      }.runTaskTimer(this.plugin, 0L, 5L); // Zmieniono z 1 ticka na 5 (0.25s) dla wydajności
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      PlayerStateManager.PlayerState state = this.plugin.getStateManager().getState(player);
      if (state == PlayerStateManager.PlayerState.HOOKED) {
         this.cancelXZMovement(event);
      }

   }

   @EventHandler(priority = EventPriority.HIGH)
   public void onPlayerSneak(PlayerToggleSneakEvent event) {
      Player player = event.getPlayer();
      PlayerStateManager.PlayerState state = this.plugin.getStateManager().getState(player);
      if (state == PlayerStateManager.PlayerState.DOWNED || state == PlayerStateManager.PlayerState.HOOKED
            || state == PlayerStateManager.PlayerState.CARRIED) {
         event.setCancelled(true);
      }

   }

   public void allowJumping(Player player) {
      this.jumpAllowed.add(player.getUniqueId());
   }

   public void disallowJumping(Player player) {
      this.jumpAllowed.remove(player.getUniqueId());
   }

   private void cancelXZMovement(PlayerMoveEvent event) {
      Location from = event.getFrom();
      Location to = event.getTo();
      if (to != null) {
         if (Math.abs(to.getX() - from.getX()) > 0.05D || Math.abs(to.getZ() - from.getZ()) > 0.05D
               || to.getY() - from.getY() > 0.05D) {
            Location cancel = from.clone();
            cancel.setYaw(to.getYaw());
            cancel.setPitch(to.getPitch());
            event.setTo(cancel);
         }

      }
   }
}
