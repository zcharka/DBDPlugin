package pl.dbd.commands;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;

public class HealAllCommand implements CommandExecutor {
   private final DBDPlugin plugin;

   public HealAllCommand(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("dbd.admin")) {
         sender.sendMessage("§cBrak permisji!");
         return true;
      } else {
         int healed = 0;

         for(Iterator var6 = Bukkit.getOnlinePlayers().iterator(); var6.hasNext(); ++healed) {
            Player online = (Player)var6.next();
            this.healPlayer(online);
            online.sendMessage(this.plugin.getConfig().getString("messages.healed", "§aZostałeś wyleczony!"));
         }

         sender.sendMessage(this.plugin.getConfig().getString("messages.healed-all", "§aWyleczono wszystkich graczy! ({count})").replace("{count}", String.valueOf(healed)));
         return true;
      }
   }

   private void healPlayer(Player player) {
      this.plugin.getStateManager().heal(player);
      player.setSwimming(false);
      player.setWalkSpeed(0.2F);
      player.setFlySpeed(0.1F);
      player.removePotionEffect(PotionEffectType.SLOWNESS);
      player.removePotionEffect(PotionEffectType.JUMP_BOOST);
      player.removePotionEffect(PotionEffectType.BLINDNESS);
      player.setVelocity(player.getVelocity().setY(0));
   }
}
