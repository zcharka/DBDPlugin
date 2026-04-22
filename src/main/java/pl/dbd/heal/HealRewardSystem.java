package pl.dbd.heal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class HealRewardSystem {
   private final DBDPlugin plugin;

   public HealRewardSystem(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void rewardHeal(Player healer, Player healed) {
      this.dispatch(healer, 1, "Wyleczenie survivora");
   }

   private void dispatch(Player player, int amount, String reason) {
      if (player != null && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}", reason));
      }
   }
}
