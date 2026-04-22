package pl.dbd.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class PAPIExpansion extends PlaceholderExpansion {

    private final DBDPlugin plugin;

    public PAPIExpansion(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "dbd";
    }

    @Override
    public String getAuthor() {
        return "Seb";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline()) return "";
        Player p = player.getPlayer();

        // %dbd_wins%
        if (params.equalsIgnoreCase("wins")) {
            return String.valueOf(plugin.getPlayerDataManager().getWins(p.getUniqueId()));
        }
        // %dbd_losses%
        if (params.equalsIgnoreCase("losses")) {
            return String.valueOf(plugin.getPlayerDataManager().getLosses(p.getUniqueId()));
        }
        // %dbd_games%
        if (params.equalsIgnoreCase("games")) {
            return String.valueOf(plugin.getPlayerDataManager().getGamesPlayed(p.getUniqueId()));
        }
        // %dbd_souls%
        if (params.equalsIgnoreCase("souls")) {
            return String.valueOf(plugin.getSoulsManager().getBalance(p));
        }
        
        // %dbd_status% (Dla scoreboarda: RANNY, ZDROWY itp.)
        if (params.equalsIgnoreCase("status")) {
            if (plugin.getStateManager().isDowned(p)) return "§cPOWALONY";
            if (plugin.getStateManager().isHooked(p)) return "§4HAK";
            if (plugin.getStateManager().isInjured(p)) return "§eRANNY";
            if (plugin.getStateManager().isCarried(p)) return "§6NIESIONY";
            return "§aZDROWY";
        }

        return null; 
    }
}