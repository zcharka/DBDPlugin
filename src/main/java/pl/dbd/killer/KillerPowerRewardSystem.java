package pl.dbd.killer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class KillerPowerRewardSystem {
   private final DBDPlugin plugin;

   public KillerPowerRewardSystem(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void rewardPowerUse(Player killer, String powerName) {
      this.dispatch(killer, 1, "Użycie mocy: " + powerName);
   }

   private void dispatch(Player p, int amount, String reason) {
      if (p != null && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}", reason));
      }
   }
}
