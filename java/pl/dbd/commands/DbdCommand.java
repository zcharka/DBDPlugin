package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.dbd.DBDPlugin;

public class DbdCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public DbdCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage(plugin.getConfig().getString("messages.no-permission", "§cBrak uprawnień!"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("mode")) {
            if (args.length < 2) {
                sender.sendMessage("§cUżycie: /dbd mode <1v4|2v8>");
                return true;
            }
            String modeStr = args[1].toLowerCase();
            if (modeStr.equals("1v4")) {
                plugin.getGameManager().set2v8Mode(false);
                plugin.getConfig().set("game-mode-2v8", false);
                plugin.saveConfig();
                sender.sendMessage("§aZmieniono tryb gry na: §e1v4 §a(1 Killer, 4 Survivorów)");
            } else if (modeStr.equals("2v8")) {
                plugin.getGameManager().set2v8Mode(true);
                plugin.getConfig().set("game-mode-2v8", true);
                plugin.saveConfig();
                sender.sendMessage("§aZmieniono tryb gry na: §c§l2v8 §a(2 Killerów, 8 Survivorów)");
            } else {
                sender.sendMessage("§cNieznany tryb. Dostępne: 1v4, 2v8");
            }
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("euycie7dbdreload"));
            return true;
        }

        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        if (plugin.getShopManager() != null)
            plugin.getShopManager().load();
        if (plugin.getEquipmentManager() != null)
            plugin.getEquipmentManager().load();
        if (plugin.getGeneratorManager() != null)
            plugin.getGeneratorManager().load();
        if (plugin.getHookManager() != null)
            plugin.getHookManager().load();
        if (plugin.getLockerManager() != null)
            plugin.getLockerManager().load();
        if (plugin.getExitGateManager() != null)
            plugin.getExitGateManager().load();
        if (plugin.getWindowManager() != null)
            plugin.getWindowManager().load();

        if (plugin.getGameManager() != null)
            plugin.getGameManager().loadMaps();
        if (plugin.getGameBanManager() != null)
            plugin.getGameBanManager().loadBans();
        if (plugin.getQuitPenaltyManager() != null)
            plugin.getQuitPenaltyManager().load();

        sender.sendMessage(pl.dbd.DBDPlugin.getMsg("adbdpomylnieprzeadow"));
        return true;
    }
}
