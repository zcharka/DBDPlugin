package pl.dbd.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.currency.BloodPointsManager;

public class DBDPlaceholder extends PlaceholderExpansion {
    private final DBDPlugin plugin;
    private final BloodPointsManager bpm;

    public DBDPlaceholder(DBDPlugin plugin, BloodPointsManager bpm) {
        this.plugin = plugin;
        this.bpm = bpm;
    }

    @Override
    public String getIdentifier() {
        return "dbd";
    }

    @Override
    public String getAuthor() {
        return "ZolloCraft";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline())
            return "0";
        Player player = offlinePlayer.getPlayer();
        if (player == null)
            return "0";

        try {
            switch (params.toLowerCase()) {
                // POPRAWKA: Pobiera teraz dusze prosto z poprawnego pliku Dusz Nexusa!
                case "dusze":
                    return String.valueOf(plugin.getSoulsManager().getBalance(player));
                case "dusze_formatted":
                    return formatShort(plugin.getSoulsManager().getBalance(player));

                case "wins":
                    return String.valueOf(bpm.getWins(player));
                case "looses":
                case "losses":
                    return String.valueOf(bpm.getLosses(player));
                case "games":
                    return String.valueOf(bpm.getWins(player) + bpm.getLosses(player));
                case "kills":
                    return String.valueOf(bpm.getKills(player));
                case "escapes":
                    return String.valueOf(bpm.getEscapes(player));

                // POPRAWKA: Tłumaczenie stanu na język polski
                case "state":
                    if (!plugin.getGameManager().isSurvivor(player)) {
                        return "";
                    }
                    return translateState(plugin.getStateManager().getState(player).name());
                default:
                    return null;
            }
        } catch (Exception e) {
            return "0";
        }
    }

    // Funkcja tłumacząca systemowy stan na ładny wygląd w GUI
    private String translateState(String stateName) {
        if (stateName == null)
            return "Brak";
        switch (stateName) {
            case "HEALTHY":
                return "Zdrowy";
            case "INJURED":
                return "Ranny";
            case "DOWNED":
                return "Powalony";
            case "HOOKED":
                return "Na haku";
            case "CARRIED":
                return "Niesiony";
            case "DEAD":
                return "Martwy";
            default:
                return stateName;
        }
    }

    private String formatShort(long value) {
        if (value < 0)
            return "-" + formatShort(-value);
        if (value < 1000)
            return String.valueOf(value);
        if (value < 1_000_000) {
            double v = value / 1000.0;
            return (v == (long) v) ? (long) v + "k" : String.format("%.1fk", v);
        }
        if (value < 1_000_000_000) {
            double v = value / 1_000_000.0;
            return (v == (long) v) ? (long) v + "M" : String.format("%.1fM", v);
        }
        double v = value / 1_000_000_000.0;
        return (v == (long) v) ? (long) v + "B" : String.format("%.1fB", v);
    }
}