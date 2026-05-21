package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralny rejestr Tab Completerów dla wszystkich komend pluginu DBD.
 * Zamiast pisać TabCompleter w każdej klasie, ta klasa obsługuje wszystko.
 */
public class TabCompleterRegistry implements TabCompleter {

    private final DBDPlugin plugin;

    public TabCompleterRegistry(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "game":
                return handleGame(args);
            case "gamemap":
                return handleGameMap(args);
            case "generator":
                return handleGenerator(args);
            case "exitgate":
                return handleExitGate(args);
            case "hook":
                return handleHook(args);
            case "window":
                return handleWindow(args);
            case "locker":
                return handleLocker(args);
            case "heal":
                return handleHeal(sender, args);
            case "healall":
                return Collections.emptyList();
            case "dbd":
                return handleDbd(args);
            case "statsreset":
                return handleStatsReset(args);
            case "blockeq":
                return Collections.emptyList();
            case "ogladaj":
                return handleOgladaj(args);
            case "gotowy":
            case "niegotowy":
            case "sklep":
            case "shopadmin":
            case "ekwipunekadmin":
            case "trycrawl":
                return Collections.emptyList();
            case "gameban":
                return handleGameBan(args);
            case "gameunban":
                return handleGameUnban(args);
            case "dbdstate":
                return handleDbdState(args);
            default:
                return null;
        }
    }

    // /game
    // <start|stop|setlobby|setkilledspawn|setescapedspawn|sethookedspawn|info|remove>
    private List<String> handleGame(String[] args) {
        if (args.length == 1) {
            return filter(
                    Arrays.asList("start", "stop", "setlobby", "setkilledspawn", "setescapedspawn", "sethookedspawn",
                            "info", "remove"),
                    args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
        }
        return Collections.emptyList();
    }

    // /gamemap <create|remove|delete|setsurvspawn|setkillspawn|list> [nazwa_mapy]
    private List<String> handleGameMap(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "remove", "delete", "setsurvspawn", "setkillspawn", "list"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("remove") || sub.equals("delete") || sub.equals("setsurvspawn")
                    || sub.equals("setkillspawn")) {
                return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
            }
        }
        return Collections.emptyList();
    }

    // /generator <add|remove|load|reset> [mapa]
    private List<String> handleGenerator(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("add", "remove", "load", "reset"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("load")) {
                return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
            }
        }
        return Collections.emptyList();
    }

    // /exitgate <create|load|list|reset> [mapa]
    private List<String> handleExitGate(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "load", "list", "reset"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create") || sub.equals("load")) {
                return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
            }
        }
        return Collections.emptyList();
    }

    // /hook <create|remove|list|reset|load> [mapa]
    private List<String> handleHook(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "remove", "list", "reset", "load"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create") || sub.equals("load")) {
                return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
            }
        }
        return Collections.emptyList();
    }

    // /window <create|remove|list>
    private List<String> handleWindow(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "remove", "list"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            int count = plugin.getWindowManager().getWindowCount();
            List<String> ids = new ArrayList<>();
            for (int i = 1; i <= count; i++) {
                ids.add(String.valueOf(i));
            }
            return filter(ids, args[1]);
        }
        return Collections.emptyList();
    }

    // /locker <create|remove|list|reset> [mapa]
    private List<String> handleLocker(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("create", "remove", "list", "reset"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("create")) {
                return filter(new ArrayList<>(plugin.getGameManager().getMaps()), args[1]);
            }
        }
        return Collections.emptyList();
    }

    // /heal [gracz|all]
    private List<String> handleHeal(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("all");
            for (Player p : Bukkit.getOnlinePlayers()) {
                suggestions.add(p.getName());
            }
            return filter(suggestions, args[0]);
        }
        return Collections.emptyList();
    }

    // /dbd <reload>
    private List<String> handleDbd(String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("reload"), args[0]);
        }
        return Collections.emptyList();
    }

    // /statsreset <gracz>
    private List<String> handleStatsReset(String[] args) {
        if (args.length == 1) {
            return filter(getOnlinePlayerNames(), args[0]);
        }
        return Collections.emptyList();
    }

    // /ogladaj [gracz]
    private List<String> handleOgladaj(String[] args) {
        if (args.length == 1) {
            return filter(getOnlinePlayerNames(), args[0]);
        }
        return Collections.emptyList();
    }

    // /gameban <nick> <czas> [powód]
    private List<String> handleGameBan(String[] args) {
        if (args.length == 1) {
            return filter(getOnlinePlayerNames(), args[0]);
        }
        if (args.length == 2) {
            String in = args[1];
            if (in.isEmpty()) {
                return Arrays.asList("1m", "5m", "10m", "1h", "1d", "7d");
            }
            if (in.matches("\\d+")) {
                return Arrays.asList(in + "m", in + "h", in + "d", in + "w", in + "mo", in + "y");
            }
            return Collections.emptyList();
        }
        if (args.length == 3) {
            return Collections.singletonList("<Powód>");
        }
        return Collections.emptyList();
    }

    // /gameunban <nick>
    private List<String> handleGameUnban(String[] args) {
        if (args.length == 1) {
            return filter(getOnlinePlayerNames(), args[0]);
        }
        return Collections.emptyList();
    }

    // /dbdstate <gracz> <stan>
    private List<String> handleDbdState(String[] args) {
        if (args.length == 1) {
            return filter(getOnlinePlayerNames(), args[0]);
        }
        if (args.length == 2) {
            return filter(Arrays.asList("downed", "injured", "healthy", "dead", "hooked", "carried", "in_locker"), args[1]);
        }
        return Collections.emptyList();
    }

    // ── UTILS ──

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String input) {
        if (input == null || input.isEmpty())
            return options;
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
