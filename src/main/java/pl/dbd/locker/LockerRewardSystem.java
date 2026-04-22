package pl.dbd.locker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class LockerRewardSystem {
   private final DBDPlugin plugin;

   public LockerRewardSystem(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void rewardHide(Player player) {
      int amount = this.plugin.getConfig().getInt("souls-rewards.survivor-hide-locker", 1);
      this.dispatch(player, amount, "Schowanie się w szafie");
   }

   private void dispatch(Player p, int amount, String reason) {
      if (p != null && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName())
               .replace("{amount}", String.valueOf(amount)).replace("{reason}", reason));
      }
   }
}
