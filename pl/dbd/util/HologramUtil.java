package pl.dbd.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

public class HologramUtil {
   private static final List<ArmorStand> holograms = new ArrayList();

   public static void spawn(JavaPlugin plugin, Location l, String text) {
      ArmorStand a = (ArmorStand)l.getWorld().spawn(l.clone().add(0.0D, 1.2D, 0.0D), ArmorStand.class);
      a.setInvisible(true);
      a.setMarker(true);
      a.setCustomNameVisible(true);
      a.setCustomName(text.replace("&", "§"));
      a.setGravity(false);
      holograms.add(a);
   }

   public static void clearAll() {
      holograms.forEach(Entity::remove);
      holograms.clear();
   }
}
