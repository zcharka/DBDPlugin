package pl.dbd.vault;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import pl.dbd.DBDPlugin;

public class WindowVaultListener implements Listener {
   private final DBDPlugin plugin;
   private final WindowVaultManager windowManager;

   public WindowVaultListener(DBDPlugin plugin, WindowVaultManager windowManager) {
      this.plugin = plugin;
      this.windowManager = windowManager;
   }

   @EventHandler
   public void onMove(PlayerMoveEvent e) {
      Player p = e.getPlayer();
      if (!p.isSneaking() && e.getTo() != null) {
         Location to = e.getTo();
         if (to.getBlock().getType() == Material.BARRIER) {
            Location windowLoc = to.getBlock().getLocation();
            if (this.windowManager.isBlocked(windowLoc)) {
               p.sendMessage("§cTo okno jest zablokowane!");
            } else {
               double speed = p.hasPermission("dbd.killer") ? this.plugin.getConfig().getDouble("window.killer-speed", 0.4D) : this.plugin.getConfig().getDouble("window.survivor-speed", 0.8D);
               Vector dir = p.getLocation().getDirection().normalize().multiply(speed);
               p.setVelocity(dir);
            }
         }
      }

   }
}
