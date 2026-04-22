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

    // KONSTRUKTOR PASUJĄCY DO TEGO W MAINIE
    public HookCommand(DBDPlugin plugin, HookManager hookManager) {
        this.plugin = plugin;
        this.hookManager = hookManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reset")) {
                hookManager.resetAllHooks();
                p.sendMessage("§aZresetowano haki.");
            } else if (args[0].equalsIgnoreCase("list")) {
                p.sendMessage("§eLiczba haków: " + hookManager.getAllHooks().size());
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (hookManager.removeHook(p.getLocation())) {
                    p.sendMessage("§aUsunięto hak.");
                } else {
                    p.sendMessage("§cBrak haka w pobliżu.");
                }
            }
        }
        return true;
    }
}