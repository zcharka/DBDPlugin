package pl.dbd.protection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.dbd.DBDPlugin;

public class UniversalProtectionListener implements Listener {
   private final DBDPlugin plugin;

   public UniversalProtectionListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.LOW
   )
   public void onDamage(EntityDamageEvent event) {
      if (event.getEntity() instanceof Player) {
         event.setCancelled(true);
      }
   }
}
