package pl.dbd.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.generator.GeneratorManager;

public class GeneratorCommand implements CommandExecutor {

    private final GeneratorManager manager;

    public GeneratorCommand(GeneratorManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (!p.hasPermission("dbd.admin"))
            return true;

        if (args.length == 0) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cgeneratoraddnazwama"));
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cgeneratorremove"));
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cgeneratorloadnazwam"));
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cgeneratorreset"));
            return true;
        }

        // /generator add <mapa>
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cpodajnazwmapynpgene"));
                return true;
            }
            String mapName = args[1];
            manager.createGenerator(p, mapName);
            return true;
        }

        // /generator load <mapa>
        if (args[0].equalsIgnoreCase("load")) {
            if (args.length < 2) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cpodajnazwmapy"));
                return true;
            }
            manager.activateMap(args[1]);
            p.sendMessage("§aZaładowano generatory dla mapy: " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (manager.removeClosestGenerator(p.getLocation(), 5.0)) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("ausunitogenerator"));
            } else {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cniepatrzysznagenera"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            manager.resetAllGenerators();
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("azresetowanopostpgen"));
            return true;
        }

        return true;
    }
}