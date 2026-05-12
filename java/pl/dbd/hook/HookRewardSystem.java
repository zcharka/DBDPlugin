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
         int amount = hookStage;
         if (hookStage == 1)
            amount = this.plugin.getConfig().getInt("souls-rewards.killer-hook-stage-1", 1);
         else if (hookStage == 2)
            amount = this.plugin.getConfig().getInt("souls-rewards.killer-hook-stage-2", 2);
         else if (hookStage >= 3)
            amount = this.plugin.getConfig().getInt("souls-rewards.killer-hook-stage-3", 3);

         this.dispatch(killer, amount, reason);
      }

   }

   public void rewardUnhooked(Player rescuer, Player rescued) {
      int rescuerAmount = this.plugin.getConfig().getInt("souls-rewards.survivor-unhook-rescuer", 1);
      int rescuedAmount = this.plugin.getConfig().getInt("souls-rewards.survivor-unhook-rescued", 1);
      this.dispatch(rescuer, rescuerAmount, "Ściągnięcie z haka");
      this.dispatch(rescued, rescuedAmount, "Zostanie ściągniętym z haka");
   }

   public void rewardKill(Player killer, Player survivor) {
      int amount = this.plugin.getConfig().getInt("souls-rewards.killer-kill", 3);
      this.dispatch(killer, amount, "Zabicie survivora");
   }

   private void dispatch(Player player, int amount, String reason) {
      if (player != null && player.isOnline() && amount > 0) {
         String cmd = this.plugin.getConfig().getString("souls.add-command", "dusze dodaj {player} {amount} {reason}");
         cmd = cmd.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount)).replace("{reason}",
               reason);
         Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
      }
   }
}
