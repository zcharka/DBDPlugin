package pl.dbd.locker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class Locker {
   private final Location doorLocation;
   private final BlockFace facing;
   private UUID playerInside;

   public Locker(Location doorLocation, BlockFace facing) {
      this.doorLocation = doorLocation;
      this.facing = facing;
   }

   public Location getDoorLocation() {
      return this.doorLocation;
   }

   public BlockFace getFacing() {
      return this.facing;
   }

   public boolean hasPlayerInside() {
      return this.playerInside != null;
   }

   public UUID getPlayerInside() {
      return this.playerInside;
   }

   public void enterLocker(Player player, DBDPlugin plugin) {
      this.playerInside = player.getUniqueId();
      player.sendMessage("§8Schowałeś się w szafie. §7Wyjdź klikając PPM lub §e/locker exit§7.");
      Location inside = this.doorLocation.clone().add(0.5D, 0.1D, 0.5D);
      inside.setYaw(this.doorLocation.getYaw() + 180.0F);
      player.teleport(inside);
      player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 10, false, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false));
      if (plugin.getStateManager() != null) {
         plugin.getStateManager().setState(player, PlayerStateManager.PlayerState.HEALTHY);
      }

   }

   public void exitLocker(Player player, DBDPlugin plugin) {
      this.playerInside = null;
      if (player != null) {
         player.removePotionEffect(PotionEffectType.INVISIBILITY);
         player.removePotionEffect(PotionEffectType.SLOWNESS);
         player.removePotionEffect(PotionEffectType.JUMP_BOOST);
         Location exit = this.doorLocation.clone().add((double)this.facing.getModX() * 1.5D, 0.0D, (double)this.facing.getModZ() * 1.5D);
         exit.setY((double)this.doorLocation.getBlockY());
         player.teleport(exit);
         player.sendMessage("§aWyszedłeś z szafy.");
      }
   }

   public Map<String, Object> serialize() {
      Map<String, Object> map = new HashMap();
      map.put("world", this.doorLocation.getWorld().getName());
      map.put("x", this.doorLocation.getX());
      map.put("y", this.doorLocation.getY());
      map.put("z", this.doorLocation.getZ());
      map.put("facing", this.facing.name());
      return map;
   }
}
