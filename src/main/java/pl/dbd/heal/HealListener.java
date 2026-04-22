package pl.dbd.heal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class HealListener implements Listener {
   private final DBDPlugin plugin;
   private final PlayerStateManager stateManager;
   private final Map<UUID, BukkitRunnable> healingTasks = new HashMap();

   public HealListener(DBDPlugin plugin, PlayerStateManager stateManager) {
      this.plugin = plugin;
      this.stateManager = stateManager;
   }

   @EventHandler
   public void onHealAttempt(PlayerInteractAtEntityEvent event) {
      if (event.getRightClicked() instanceof Player) {
         final Player healer = event.getPlayer();
         final Player target = (Player)event.getRightClicked();
         if (healer.hasPermission("dbd.survivor")) {
            if (target.hasPermission("dbd.survivor")) {
               if (this.stateManager.isInjured(target)) {
                  if (!healer.getUniqueId().equals(target.getUniqueId())) {
                     if (!this.stateManager.isCarried(target) && !this.stateManager.isHooked(target)) {
                        event.setCancelled(true);
                        if (this.healingTasks.containsKey(healer.getUniqueId())) {
                           healer.sendMessage("§cJuż leczysz kogoś!");
                        } else {
                           healer.sendMessage("§aLeczysz: §e" + target.getName() + "§a... (4s)");
                           target.sendMessage("§aJesteś leczony przez: §e" + healer.getName());
                           BukkitRunnable task = new BukkitRunnable() {
                              int ticks = 0;
                              final int TOTAL = 80;

                              public void run() {
                                 if (healer.isOnline() && target.isOnline() && !(healer.getLocation().distanceSquared(target.getLocation()) > 9.0D)) {
                                    ++this.ticks;
                                    int pct = this.ticks * 100 / 80;
                                    healer.sendActionBar("§aLeczenie: §e" + pct + "%");
                                    target.sendActionBar("§aLeczony: §e" + pct + "%");
                                    if (this.ticks >= 80) {
                                       HealListener.this.stateManager.heal(target);
                                       target.removePotionEffect(PotionEffectType.SLOWNESS);
                                       target.setSwimming(false);
                                       target.setWalkSpeed(0.2F);
                                       healer.sendMessage("§aWyleczono: §e" + target.getName());
                                       target.sendMessage("§aZostałeś wyleczony przez: §e" + healer.getName());
                                       target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                                       HealListener.this.dispatchSouls(healer, 1, "Wyleczenie survivora");
                                       HealListener.this.healingTasks.remove(healer.getUniqueId());
                                       this.cancel();
                                    }

                                 } else {
                                    healer.sendMessage("§cLeczenie przerwane!");
                                    HealListener.this.healingTasks.remove(healer.getUniqueId());
                                    this.cancel();
                                 }
                              }
                           };
                           task.runTaskTimer(this.plugin, 0L, 1L);
                           this.healingTasks.put(healer.getUniqueId(), task);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void dispatchSouls(Player player, int amount, String reason) {
      String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
      cmd = cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}", reason);
      Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
   }
}
