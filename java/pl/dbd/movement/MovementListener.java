package pl.dbd.movement;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import pl.dbd.DBDPlugin;

public class MovementListener implements Listener {
   private final DBDPlugin plugin;

   public MovementListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();

      if (this.plugin.getStateManager() != null && this.plugin.getStateManager().isDowned(player)) {
         // Optymalizacja: ustawiaj pozę tylko, jeśli nie jest już ustawiona
         if (player.getPose() != org.bukkit.entity.Pose.SWIMMING) {
            player.setPose(org.bukkit.entity.Pose.SWIMMING, false);
         }
      }

      if (player.getGameMode() != GameMode.SPECTATOR && player.getGameMode() != GameMode.CREATIVE
            && (player.hasPermission("dbd.survivor") || player.hasPermission("dbd.killer"))) {

         // Blokada wspinaczki po lianach (tylko ruch w górę)
         if (player.isClimbing() && event.getTo() != null && event.getFrom() != null
               && event.getTo().getY() > event.getFrom().getY()) {
            event.setTo(event.getFrom());
            player.sendActionBar("§cNie możesz się wspinać!");
         }
      }

   }

   @EventHandler
   public void onPlayerSneak(PlayerToggleSneakEvent event) {
      Player player = event.getPlayer();
      if (this.plugin.getStateManager() != null && this.plugin.getStateManager().isDowned(player)) {
         event.setCancelled(true);
         player.setPose(org.bukkit.entity.Pose.SWIMMING, false);
         return;
      }

      if (player.isClimbing() && event.isSneaking()) {
         // Pozwól na przytrzymanie Shift obok lian
      }
   }

   @EventHandler
   public void onPlayerToggleSwim(EntityToggleSwimEvent event) {
      if (event.getEntity() instanceof Player) {
         Player player = (Player) event.getEntity();
         if (this.plugin.getStateManager() != null && this.plugin.getStateManager().isDowned(player)) {
            if (!event.isSwimming()) {
               event.setCancelled(true);
               player.setPose(org.bukkit.entity.Pose.SWIMMING, false);
            }
         }
      }
   }
}
