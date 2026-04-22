package pl.dbd.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class HookRewardSystem {
   private final DBDPlugin plugin;

   public HookRewardSystem(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void rewardHooked(Player killer, Player survivor, int hookStage) {
      String reason = "Powieszenie (Etap " + hookStage + ")";
      if (hookStage > 0 && killer != null) {
         this.dispatch(killer, hookStage, reason);
      }

   }

   public void rewardUnhooked(Player rescuer, Player rescued) {
      this.dispatch(rescuer, 1, "Ściągnięcie z haka");
      this.dispatch(rescued, 1, "Zostanie ściągniętym z haka");
   }

   public void rewardKill(Player killer, Player survivor) {
      this.dispatch(killer, 3, "Zabicie survivora");
   }

   private void dispatch(Player player, int amount, String reason) {
      if (player != null && player.isOnline() && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         cmd = cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}", reason);
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
      }
   }
}
