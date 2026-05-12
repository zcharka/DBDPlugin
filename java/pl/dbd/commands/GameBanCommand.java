package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.dbd.DBDPlugin;

import java.util.UUID;

public class GameBanCommand implements CommandExecutor {
    private final DBDPlugin plugin;

    public GameBanCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage("§cBrak uprawnień!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /gameban <nick> <czas w minutach> [powód]");
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage("§cGracz nie znaleziony!");
            return true;
        }

        long durationMs = parseDuration(args[1]);
        if (durationMs < 0) {
            sender.sendMessage("§cPodano błędny format czasu (np. 10m, 5h, 14d)!");
            return true;
        }
        String reason = "Brak powodu";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            reason = sb.toString().trim();
        }

        UUID uuid = target.getUniqueId();
        if (plugin.getGameBanManager() != null) {
            plugin.getGameBanManager().banPlayer(uuid, durationMs, reason);
            sender.sendMessage("§aPomyślnie zbanowano gracza §e" + target.getName() + " §az rozgrywki na czas: §e"
                    + args[1] + "§a!");

            // Jeśli gracz jest powiązany z LOBBY, może wyrzucić z LOBBY:
            if (target.isOnline() && target.getPlayer() != null) {
                if (plugin.getGameManager() != null) {
                    if (plugin.getGameManager().getQueuedPlayers().contains(uuid)
                            || plugin.getGameManager().getReadyPlayers().contains(uuid)) {
                        plugin.getGameManager().leaveLobby(target.getPlayer());
                        target.getPlayer().sendMessage("§cZostałeś wyrzucony z kolejki! Jesteś zbanowany z rozgrywki.");
                    }
                }
            }
        } else {
            sender.sendMessage("§cGameBanManager jest niezainicjalizowany!");
        }

        return true;
    }

    private long parseDuration(String input) {
        long totalMs = 0;
        StringBuilder currentNumber = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else {
                if (currentNumber.length() == 0)
                    continue;
                long val = Long.parseLong(currentNumber.toString());
                currentNumber.setLength(0);

                StringBuilder unitStr = new StringBuilder();
                while (i < input.length() && Character.isLetter(input.charAt(i))) {
                    unitStr.append(input.charAt(i));
                    i++;
                }
                i--; // cofte po wczytaniu liter

                switch (unitStr.toString().toLowerCase()) {
                    case "s":
                        totalMs += val * 1000L;
                        break;
                    case "m":
                        totalMs += val * 60L * 1000L;
                        break;
                    case "h":
                        totalMs += val * 60L * 60L * 1000L;
                        break;
                    case "d":
                        totalMs += val * 24L * 60L * 60L * 1000L;
                        break;
                    case "w":
                        totalMs += val * 7L * 24L * 60L * 60L * 1000L;
                        break;
                    case "mo":
                        totalMs += val * 30L * 24L * 60L * 60L * 1000L;
                        break;
                    case "y":
                        totalMs += val * 365L * 24L * 60L * 60L * 1000L;
                        break;
                    default:
                        totalMs += val * 60L * 1000L; // domyślnie minuty jeżeli nieznane
                }
            }
        }
        if (currentNumber.length() > 0) {
            totalMs += Long.parseLong(currentNumber.toString()) * 60L * 1000L;
        }
        return totalMs;
    }
}
