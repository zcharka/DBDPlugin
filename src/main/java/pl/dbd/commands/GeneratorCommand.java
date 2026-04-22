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
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("dbd.admin")) return true;

        if (args.length == 0) {
            p.sendMessage("§c/generator add <NazwaMapy>");
            p.sendMessage("§c/generator remove");
            p.sendMessage("§c/generator load <NazwaMapy>");
            p.sendMessage("§c/generator reset");
            return true;
        }

        // /generator add <mapa>
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                p.sendMessage("§cPodaj nazwę mapy! Np. /generator add farma");
                return true;
            }
            String mapName = args[1];
            manager.createGenerator(p, mapName); 
            return true;
        }

        // /generator load <mapa>
        if (args[0].equalsIgnoreCase("load")) {
            if (args.length < 2) {
                p.sendMessage("§cPodaj nazwę mapy!");
                return true;
            }
            manager.activateMap(args[1]);
            p.sendMessage("§aZaładowano generatory dla mapy: " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (manager.removeGenerator(p.getTargetBlock(null, 5).getLocation())) {
                p.sendMessage("§aUsunięto generator.");
            } else {
                p.sendMessage("§cNie patrzysz na generator.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            manager.resetAllGenerators();
            p.sendMessage("§aZresetowano postęp generatorów.");
            return true;
        }

        return true;
    }
}