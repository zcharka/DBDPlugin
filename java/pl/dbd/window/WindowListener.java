package pl.dbd.window;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import pl.dbd.DBDPlugin;

public class WindowListener implements Listener {
   private final DBDPlugin plugin;
   private final WindowManager manager;

   public WindowListener(DBDPlugin plugin, WindowManager manager) {
      this.plugin = plugin;
      this.manager = manager;
   }

   @EventHandler
   public void onMove(PlayerMoveEvent e) {
      Player p = e.getPlayer();
      if (e.getTo() != null) {
         Location to = e.getTo().getBlock().getLocation();
         if (this.manager.isWindow(to)) {
            double speed = p.hasPermission("dbd.killer") ? this.plugin.getConfig().getDouble("window.killer-speed", 0.4D) : this.plugin.getConfig().getDouble("window.survivor-speed", 0.8D);
            Vector v = p.getLocation().getDirection().normalize().multiply(speed);
            p.setVelocity(v);
         }
      }

   }
}
