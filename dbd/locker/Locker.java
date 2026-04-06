package pl.dbd.locker;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class Locker {
   private final Location doorLocation; // dolna część żelaznych drzwi
   private UUID playerInside;

   public Locker(Location doorLocation) {
      this.doorLocation = doorLocation;
   }

   public Location getDoorLocation() {
      return this.doorLocation;
   }

   public boolean hasPlayerInside() {
      return this.playerInside != null;
   }

   public UUID getPlayerInside() {
      return this.playerInside;
   }

   public void enterLocker(Player player, DBDPlugin plugin) {
      this.playerInside = player.getUniqueId();
      player.sendMessage(pl.dbd.DBDPlugin.getMsg("8schowaesiwszafie7wy"));

      // Teleportuj gracza do środka drzwi
      Location inside = this.doorLocation.clone().add(0.5D, 0.1D, 0.5D);
      inside.setYaw(player.getLocation().getYaw());
      player.teleport(inside);

      // Ukryj gracza przed wszystkimi (nametag + model)
      for (Player other : Bukkit.getOnlinePlayers()) {
         if (!other.equals(player)) {
            other.hidePlayer(plugin, player);
         }
      }

      // Efekty: niewidzialność, spowolnienie, blokada skoku
      // Używamy bezpiecznej dużej wartości (999999 ticków ≈ 13h) zamiast MAX_VALUE,
      // co czasem powoduje błędy
      player.removePotionEffect(PotionEffectType.INVISIBILITY);
      player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 0, false, false));

      player.removePotionEffect(PotionEffectType.SLOWNESS);
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 255, false, false));
      player.removePotionEffect(PotionEffectType.JUMP_BOOST);
      player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 999999, 128, false, false));
      player.setWalkSpeed(0.0F);
      player.setCollidable(false);

      // Stan IN_LOCKER (zapobiega resetowi prędkości przez stateEnforcementTask)
      if (plugin.getStateManager() != null) {
         plugin.getStateManager().setState(player, PlayerStateManager.PlayerState.IN_LOCKER);
      }

      // Dźwięk zamknięcia
      if (player.isSprinting()) {
         doorLocation.getWorld().playSound(doorLocation, Sound.BLOCK_IRON_DOOR_CLOSE, 1.5f, 0.7f);
      } else {
         doorLocation.getWorld().playSound(doorLocation, Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.2f);
      }
   }

   public void exitLocker(Player player, DBDPlugin plugin) {
      this.playerInside = null;
      if (player != null) {
         // Odkryj gracza
         for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
               other.showPlayer(plugin, player);
            }
         }

         // Zdejmij efekty
         player.removePotionEffect(PotionEffectType.INVISIBILITY);
         player.removePotionEffect(PotionEffectType.SLOWNESS);
         player.removePotionEffect(PotionEffectType.JUMP_BOOST);
         player.setWalkSpeed(0.2F);
         player.setCollidable(true);

         // Teleportuj przed szafkę
         Location exit = this.doorLocation.clone().add(0.5D, 0.0D, 0.5D);
         player.teleport(exit);

         // Stan HEALTHY
         if (plugin.getStateManager() != null) {
            plugin.getStateManager().setState(player, PlayerStateManager.PlayerState.HEALTHY);
         }

         // Dźwięk otwarcia
         doorLocation.getWorld().playSound(doorLocation, Sound.BLOCK_IRON_DOOR_OPEN, 0.8f, 1.2f);

         player.sendMessage(pl.dbd.DBDPlugin.getMsg("awyszedezszafy"));
      }
   }

   /**
    * Killer wyciąga survivora z szafy
    */
   public Player grabPlayer(Player killer, DBDPlugin plugin) {
      if (this.playerInside == null)
         return null;
      Player survivor = Bukkit.getPlayer(this.playerInside);
      if (survivor == null) {
         this.playerInside = null;
         return null;
      }

      this.playerInside = null;

      // Odkryj survivora
      for (Player other : Bukkit.getOnlinePlayers()) {
         if (!other.equals(survivor)) {
            other.showPlayer(plugin, survivor);
         }
      }

      // Zdejmij efekty
      survivor.removePotionEffect(PotionEffectType.INVISIBILITY);
      survivor.removePotionEffect(PotionEffectType.SLOWNESS);
      survivor.removePotionEffect(PotionEffectType.JUMP_BOOST);
      survivor.setWalkSpeed(0.2F);
      survivor.setCollidable(true);

      // Stan CARRIED, make ride killer
      if (plugin.getStateManager() != null) {
         plugin.getStateManager().setState(survivor, PlayerStateManager.PlayerState.CARRIED);
      }

      // Dźwięk złapania
      doorLocation.getWorld().playSound(doorLocation, Sound.BLOCK_IRON_DOOR_OPEN, 1.5f, 0.5f);
      doorLocation.getWorld().playSound(doorLocation, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);

      killer.sendMessage("§6§lZŁAPANO SURVIVORA!");
      survivor.sendMessage("§c§lZostałeś wyciągnięty z szafy!");

      // Podnieś survivora (carry system)
      if (plugin.getCarrySystem() != null) {
         plugin.getCarrySystem().pickUp(killer, survivor);
      }

      return survivor;
   }
}
