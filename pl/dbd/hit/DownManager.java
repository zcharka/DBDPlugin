package pl.dbd.hit;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DownManager {
   private final Set<UUID> downed = new HashSet();

   public boolean isDowned(Player p) {
      return this.downed.contains(p.getUniqueId());
   }

   public void down(Player p) {
      this.downed.add(p.getUniqueId());
      p.setHealth(2.0D);
      p.setVelocity(new Vector(0, 0, 0));
      p.setSwimming(true);
      p.setWalkSpeed(0.0F);
      p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 999999, 10, false, false));
      p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 999999, 128, false, false));
   }

   public void revive(Player p) {
      this.downed.remove(p.getUniqueId());
      p.setSwimming(false);
      p.setWalkSpeed(0.2F);
      p.removePotionEffect(PotionEffectType.SLOWNESS);
      p.removePotionEffect(PotionEffectType.JUMP_BOOST);
   }
}
