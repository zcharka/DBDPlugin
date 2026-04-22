package pl.dbd.movement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;

public class MovementListener implements Listener {
   private final DBDPlugin plugin;
   private final Map<UUID, Long> jumpCooldowns = new HashMap();
   private final Map<UUID, Long> walkSlowdowns = new HashMap();
   private final Map<UUID, Double> lastYPositions = new HashMap();

   public MovementListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() != GameMode.SPECTATOR && player.getGameMode() != GameMode.CREATIVE && (player.hasPermission("dbd.survivor") || player.hasPermission("dbd.killer"))) {
         if (event.getTo().getY() > event.getFrom().getY()) {
            double jumpHeight = event.getTo().getY() - event.getFrom().getY();
            if (jumpHeight > 0.1D && jumpHeight < 0.5D) {
               boolean nearWindow = false;
               if (!nearWindow) {
                  event.setCancelled(true);
                  player.sendActionBar("§cNie możesz skakać!");
                  long now = System.currentTimeMillis();
                  Long lastJump = (Long)this.jumpCooldowns.get(player.getUniqueId());
                  if (lastJump == null || now - lastJump > 1000L) {
                     player.sendMessage("§cSkakanie jest zablokowane! Użyj okien aby przeskoczyć.");
                     this.jumpCooldowns.put(player.getUniqueId(), now);
                  }
               }
            }
         }

         if (player.isClimbing()) {
            event.setCancelled(true);
            player.sendActionBar("§cNie możesz się wspinać!");
         }

         this.lastYPositions.put(player.getUniqueId(), event.getTo().getY());
      }

   }

   @EventHandler
   public void onFallDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player && event.getCause() == DamageCause.FALL) {
         Player player = (Player)event.getEntity();
         if (player.getGameMode() != GameMode.SPECTATOR && player.getGameMode() != GameMode.CREATIVE && (player.hasPermission("dbd.survivor") || player.hasPermission("dbd.killer"))) {
            Double lastY = (Double)this.lastYPositions.get(player.getUniqueId());
            if (lastY == null) {
               lastY = player.getLocation().getY();
            }

            double fallDistance = (double)player.getFallDistance();
            event.setCancelled(true);
            double newHealth;
            if (fallDistance >= 15.0D) {
               player.sendMessage("§c§lUpadek z dużej wysokości!");
               player.sendTitle("§c§lLEŻY", "§7Spadłeś z 15+ bloków", 10, 50, 20);
               if (this.plugin.getStateManager() != null) {
                  this.plugin.getStateManager().setDowned(player.getUniqueId());
               }

               player.setSneaking(true);
               player.setSwimming(true);
               player.setWalkSpeed(0.05F);
               player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 10, false, false));
               player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 999999, 250, false, false));
               newHealth = player.getHealth() - 8.0D;
               if (newHealth < 2.0D) {
                  newHealth = 2.0D;
               }

               player.setHealth(newHealth);
            } else if (fallDistance >= 12.0D) {
               player.sendMessage("§c§lUpadek! Jesteś ranny!");
               player.sendTitle("§c§lRANNY", "§7Spadłeś z 12+ bloków", 10, 50, 20);
               if (this.plugin.getStateManager() != null) {
                  this.plugin.getStateManager().setInjured(player.getUniqueId());
               }

               newHealth = player.getHealth() - 10.0D;
               if (newHealth < 1.0D) {
                  newHealth = 1.0D;
               }

               player.setHealth(newHealth);
            } else if (fallDistance >= 8.0D) {
               player.sendMessage("§e§lUpadek! Musisz otrząsnąć się.");
               player.sendActionBar("§eCooldown chodzenia: 1s");
               this.walkSlowdowns.put(player.getUniqueId(), System.currentTimeMillis() + 1000L);
               float originalSpeed = player.getWalkSpeed();
               player.setWalkSpeed(0.05F);
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  Long cooldownEnd = (Long)this.walkSlowdowns.get(player.getUniqueId());
                  if (cooldownEnd != null && System.currentTimeMillis() >= cooldownEnd) {
                     player.setWalkSpeed(originalSpeed);
                     player.sendActionBar("§aMożesz już normalnie chodzić");
                     this.walkSlowdowns.remove(player.getUniqueId());
                  }

               }, 20L);
            }

            this.lastYPositions.remove(player.getUniqueId());
         }
      }

   }

   @EventHandler
   public void onPlayerSneak(PlayerToggleSneakEvent event) {
      Player player = event.getPlayer();
      if (player.isClimbing() && event.isSneaking()) {
         event.setCancelled(true);
      }

   }

   private boolean isNearWindow(Player player) {
      return false;
   }
}
