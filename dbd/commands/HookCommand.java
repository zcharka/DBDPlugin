package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.hook.Hook;
import pl.dbd.hook.HookManager;

public class HookCommand implements CommandExecutor {
    private final DBDPlugin plugin;
    private final HookManager hookManager;

    public HookCommand(DBDPlugin plugin, HookManager hookManager) {
        this.plugin = plugin;
        this.hookManager = hookManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (!p.hasPermission("dbd.admin"))
            return true;

        if (args.length == 0) {
            p.sendMessage("§cOpcje: /hook <create|remove|list|reset|load> [mapa]");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                p.sendMessage("§cUżycie: /hook create <nazwa_mapy>");
                return true;
            }
            String mapName = args[1];
            org.bukkit.block.Block target = p.getTargetBlockExact(5);
            if (target == null) {
                p.sendMessage("§cMusisz patrzeć na jakiś blok (max 5 kratek)!");
                return true;
            }
            boolean created = hookManager.createHook(target.getLocation(), mapName);
            if (!created) {
                p.sendMessage("§cNa tym bloku jest już zarejestrowany hak dla tej mapy!");
                return true;
            }
            p.sendMessage("§aStworzono hak na mapie §e" + mapName + " §a(" + target.getType().name() + ")!");
        } else if (args[0].equalsIgnoreCase("load")) {
            if (args.length < 2) {
                p.sendMessage("§cUżycie: /hook load <nazwa_mapy>");
                return true;
            }
            hookManager.activateMap(args[1]);
            p.sendMessage(
                    "§aZaładowano haki dla mapy: §e" + args[1] + " §a(" + hookManager.getAllHooks().size() + " haków)");
        } else if (args[0].equalsIgnoreCase("reset")) {
            hookManager.resetAllHooks();
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("azresetowanohaki"));
        } else if (args[0].equalsIgnoreCase("list")) {
            p.sendMessage("§eLiczba aktywnych haków: " + hookManager.getAllHooks().size());
            p.sendMessage("§7Mapy z hakami: " + String.join(", ", hookManager.getMapNames()));
            if (!hookManager.getAllHooks().isEmpty()) {
                p.sendMessage("§aAktywne haki na tej mapie:");
                for (Hook h : hookManager.getAllHooks()) {
                    org.bukkit.Location loc = h.getLocation();
                    p.sendMessage(
                            "§7- §fX: " + loc.getBlockX() + ", Y: " + loc.getBlockY() + ", Z: " + loc.getBlockZ());
                }
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            org.bukkit.block.Block target = p.getTargetBlockExact(5);
            if (target == null) {
                p.sendMessage("§cMusisz patrzeć na hak!");
                return true;
            }
            if (hookManager.removeHook(target.getLocation())) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("ausunitohak"));
            } else {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakhakawpobliu"));
            }
        } else {
            p.sendMessage("§cOpcje: /hook <create|remove|list|reset|load> [mapa]");
        }
        return true;
    }
}