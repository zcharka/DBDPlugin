package pl.dbd.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TryCrawlCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            try {
                player.sendMessage("§ePróba wejścia w Pose.SWIMMING...");
                player.setPose(org.bukkit.entity.Pose.SWIMMING, false);
            } catch (Exception e) {
                player.sendMessage("§cTen serwer nie obsługuje metody player.setPose() z Paper API.");
            }
            return true;
        }
        return false;
    }
}
