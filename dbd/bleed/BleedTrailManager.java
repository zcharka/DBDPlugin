package pl.dbd.bleed;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class BleedTrailManager {
   private final DBDPlugin plugin;
   private final PlayerStateManager stateManager;

   public BleedTrailManager(DBDPlugin plugin, PlayerStateManager stateManager) {
      this.plugin = plugin;
      this.stateManager = stateManager;
      this.startBleedTrailTask();
   }

   private void startBleedTrailTask() {
      (new BukkitRunnable() {
         public void run() {
            Iterator var1 = Bukkit.getOnlinePlayers().iterator();

            while(true) {
               Player player;
               PlayerStateManager.PlayerState state;
               do {
                  do {
                     if (!var1.hasNext()) {
                        return;
                     }

                     player = (Player)var1.next();
                  } while(player.hasMetadata("NPC"));

                  state = BleedTrailManager.this.stateManager.getState(player);
               } while(state != PlayerStateManager.PlayerState.INJURED && state != PlayerStateManager.PlayerState.DOWNED);

               BleedTrailManager.this.createBleedTrail(player);
            }
         }
      }).runTaskTimer(this.plugin, 0L, 10L);
   }

   private void createBleedTrail(Player player) {
      Location loc = player.getLocation();
      Location bloodSource = loc.clone().add(0.0D, 1.2D, 0.0D);
      player.getWorld().spawnParticle(Particle.DUST, bloodSource, 8, 0.2D, 0.1D, 0.2D, new DustOptions(Color.fromRGB(180, 0, 0), 1.2F));
      player.getWorld().spawnParticle(Particle.DUST, bloodSource, 5, 0.15D, 0.05D, 0.15D, new DustOptions(Color.fromRGB(120, 0, 0), 0.9F));
      Location groundLoc = loc.clone();
      groundLoc.setY((double)loc.getBlockY() + 0.05D);
      player.getWorld().spawnParticle(Particle.DUST, groundLoc, 12, 0.3D, 0.02D, 0.3D, new DustOptions(Color.fromRGB(160, 0, 0), 1.5F));
      PlayerStateManager.PlayerState state = this.stateManager.getState(player);
      if (state == PlayerStateManager.PlayerState.DOWNED) {
         player.getWorld().spawnParticle(Particle.DUST, groundLoc.clone().add(0.0D, 0.1D, 0.0D), 15, 0.4D, 0.03D, 0.4D, new DustOptions(Color.fromRGB(140, 0, 0), 1.8F));
         if (Math.random() < 0.3D) {
            Location dripLoc = bloodSource.clone();

            for(int i = 0; i < 5; ++i) {
               Location particleLoc = dripLoc.clone().subtract(0.0D, (double)i * 0.2D, 0.0D);
               int delay = i * 2;
               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.01D, 0.01D, 0.01D, new DustOptions(Color.fromRGB(200, 0, 0), 1.0F));
               }, (long)delay);
            }
         }
      }

   }
}
