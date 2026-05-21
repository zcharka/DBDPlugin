package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EquipmentCommand implements CommandExecutor, TabCompleter {

    private final DBDPlugin plugin;

    public EquipmentCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // --- OBSŁUGA KOMENDY ODBLOKUJ ---
        if (args.length >= 2 && args[0].equalsIgnoreCase("odblokuj")) {
            if (!sender.hasPermission("dbd.admin")) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakuprawnie1"));
                return true;
            }

            Player target;
            String perkId;

            // Format: /ekwipunek odblokuj [perk] (dla siebie)
            if (args.length == 2 && sender instanceof Player) {
                target = (Player) sender;
                perkId = args[1];
            }
            // Format: /ekwipunek odblokuj [gracz] [perk]
            else if (args.length == 3) {
                target = Bukkit.getPlayer(args[1]);
                perkId = args[2];
            } else {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("cuycieekwipunekodblo"));
                return true;
            }

            if (target == null) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("cgraczjestoffline"));
                return true;
            }

            pl.dbd.equipment.EqEntry entry = plugin.getEquipmentManager().getEntry(perkId);
            String displayName = entry != null ? entry.display() : perkId;

            if (plugin.getEquipmentManager().unlockPerk(target, perkId)) {
                sender.sendMessage("§aPomyślnie odblokowano §e" + displayName + " §adla gracza §e" + target.getName());
            } else {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctenperknieistniejel"));
            }
            return true;
        }

        // --- OBSŁUGA KOMENDY ZABLOKUJ ---
        if (args.length >= 2 && args[0].equalsIgnoreCase("zablokuj")) {
            if (!sender.hasPermission("dbd.admin")) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("cbrakuprawnie1"));
                return true;
            }

            Player target;
            String perkId;

            // Format: /ekwipunek zablokuj [perk] (dla siebie)
            if (args.length == 2 && sender instanceof Player) {
                target = (Player) sender;
                perkId = args[1];
            }
            // Format: /ekwipunek zablokuj [gracz] [perk]
            else if (args.length == 3) {
                target = Bukkit.getPlayer(args[1]);
                perkId = args[2];
            } else {
                sender.sendMessage("§cUżycie: /ekwipunek zablokuj [gracz] <perk_id>");
                return true;
            }

            if (target == null) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("cgraczjestoffline"));
                return true;
            }

            pl.dbd.equipment.EqEntry entry = plugin.getEquipmentManager().getEntry(perkId);
            String displayName = entry != null ? entry.display() : perkId;

            if (plugin.getEquipmentManager().lockPerk(target, perkId)) {
                sender.sendMessage("§cZablokowano §e" + displayName + " §cdla gracza §e" + target.getName());
            } else {
                sender.sendMessage("§cTen perk nie istnieje lub gracz go nie posiada!");
            }
            return true;
        }

        // --- GŁÓWNA KOMENDA (OTWIERANIE GUI) ---
        if (sender instanceof Player p) {
            if (args.length == 1) {
                String sub = args[0].toLowerCase();
                pl.dbd.equipment.EquipmentGUI gui = plugin.getEquipmentManager().getGui();
                switch (sub) {
                    case "survperks":
                    case "perki_ocalalego":
                        gui.openSurvivorPerks(p);
                        return true;
                    case "survitems":
                    case "przedmioty_ocalalego":
                        gui.openSurvivorItems(p);
                        return true;
                    case "killerperks":
                    case "perki_killera":
                        gui.openKillerPerks(p);
                        return true;
                    case "killers":
                    case "zabojcy":
                        gui.openKillersMenu(p);
                        return true;
                    case "upgrade":
                    case "ulepszanie":
                        gui.openUpgradeMenu(p);
                        return true;
                    default:
                        break;
                }
            }
            plugin.getEquipmentManager().getGui().openMainMenu(p);
        } else {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctylkograczmoeotworz"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] args) {
        if (args.length == 1 && s.hasPermission("dbd.admin")) {
            return filter(Arrays.asList("odblokuj", "zablokuj"), args[0]);
        }
        if (args.length == 2 && s.hasPermission("dbd.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("odblokuj") || sub.equals("zablokuj")) {
                // Sugeruj nicki graczy online
                return filter(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()), args[1]);
            }
        }
        if (args.length == 3 && s.hasPermission("dbd.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("odblokuj")) {
                // Sugeruj wszystkie ID perków/przedmiotów z equipment.yml
                return filter(new ArrayList<>(plugin.getEquipmentManager().getAllRegisteredIds()), args[2]);
            }
            if (sub.equals("zablokuj")) {
                // Sugeruj tylko odblokowane perki tego gracza
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    return filter(new ArrayList<>(plugin.getEquipmentManager().getUnlockedPerkIds(target)), args[2]);
                }
                return List.of();
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        if (input == null || input.isEmpty())
            return options;
        String lower = input.toLowerCase();
        return options.stream()
                .filter(opt -> opt.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}