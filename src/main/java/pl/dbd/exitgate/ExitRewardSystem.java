package pl.dbd.exitgate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class ExitRewardSystem {
   private final DBDPlugin plugin;

   public ExitRewardSystem(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void rewardEscape(Player player) {
      int amount = this.plugin.getConfig().getInt("souls-rewards.survivor-exit-gate-escape", 3);
      this.dispatch(player, amount, "Ucieczka przez bramę");
   }

   public void rewardOpen(Player player) {
      int amount = this.plugin.getConfig().getInt("souls-rewards.survivor-exit-gate-open", 2);
      this.dispatch(player, amount, "Otwarcie bramy wyjściowej");
   }

   private void dispatch(Player p, int amount, String reason) {
      if (p != null && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", p.getName())
               .replace("{amount}", String.valueOf(amount)).replace("{reason}", reason));
      }
   }
}
