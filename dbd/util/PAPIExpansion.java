package pl.dbd.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

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
        return "DBDTeam";
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
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || !player.isOnline())
            return "";
        Player p = player.getPlayer();

        DBDPlugin plugin = DBDPlugin.getInstance();
        if (plugin == null || !plugin.isEnabled())
            return "";

        String lower = params.toLowerCase();

        if (lower.equals("wins") || lower.equals("wygrane")) {
            return String.valueOf(plugin.getPlayerDataManager().getWins(p.getUniqueId()));
        }

        if (lower.equals("losses") || lower.equals("looses") || lower.equals("przegrane")) {
            return String.valueOf(plugin.getPlayerDataManager().getLosses(p.getUniqueId()));
        }

        if (lower.equals("games") || lower.equals("rozegrane")) {
            return String.valueOf(plugin.getPlayerDataManager().getGamesPlayed(p.getUniqueId()));
        }

        if (lower.equals("souls") || lower.equals("dusze")) {
            return String.valueOf(plugin.getSoulsManager().getBalance(p));
        }

        if (lower.equals("dusze_formatted") || lower.equals("souls_formatted")) {
            long val = plugin.getSoulsManager().getBalance(p);
            if (val < 1000)
                return String.valueOf(val);
            if (val < 1_000_000) {
                double v = val / 1000.0;
                return (v == (long) v) ? (long) v + "k" : String.format("%.1fk", v);
            }
            if (val < 1_000_000_000) {
                double v = val / 1_000_000.0;
                return (v == (long) v) ? (long) v + "M" : String.format("%.1fM", v);
            }
            double v = val / 1_000_000_000.0;
            return (v == (long) v) ? (long) v + "B" : String.format("%.1fB", v);
        }

        if (lower.equals("status") || lower.equals("state") || lower.equals("stan")) {
            if (!plugin.getGameManager().isSurvivor(p)) {
                return "";
            }

            if (plugin.getStateManager().getState(p) == pl.dbd.state.PlayerStateManager.PlayerState.DEAD ||
                    plugin.getGameManager().isDead(p)) {
                return "§8MARTWY";
            }

            if (plugin.getStateManager().isDowned(p))
                return "§c§lᴘᴏᴡᴀʟᴏɴʏ";
            if (plugin.getStateManager().isHooked(p))
                return "§4§lɴᴀ ʜᴀᴋুকে";
            if (plugin.getStateManager().isInjured(p))
                return "§e§lʀᴀɴɴʏ";
            if (plugin.getStateManager().isCarried(p))
                return "§6§lɴɪᴇꜱɪᴏɴʏ";

            return "§a§lᴢᴅʀᴏᴡʏ";
        }

        if (lower.equals("role") || lower.equals("rola")) {
            if (plugin.getGameManager().isKiller(p))
                return "Killer";
            if (plugin.getGameManager().isSurvivor(p))
                return "Survivor";
            return "Lobby";
        }

        if (lower.startsWith("top_") || lower.startsWith("pos_")) {
            return handleTopPlaceholders(lower, p, plugin);
        }

        return null;
    }

    private String handleTopPlaceholders(String params, Player p, DBDPlugin plugin) {
        if (params.startsWith("top_")) {
            String[] parts = params.split("_");
            if (parts.length == 4) {
                String type = parts[1];
                int pos = 1;
                try {
                    pos = Integer.parseInt(parts[2]);
                } catch (Exception ignored) {
                }
                boolean wantName = parts[3].equals("name");

                if (type.equals("souls")) {
                    return getTopStringElement(plugin.getSoulsManager().getTopPlayers(3), pos, wantName);
                } else if (type.equals("games")) {
                    return getTopUUIDElement(plugin.getPlayerDataManager().getTopPlayersByGames(3), pos, wantName);
                } else if (type.equals("wins")) {
                    return getTopUUIDElement(plugin.getPlayerDataManager().getTopPlayersByWins(3), pos, wantName);
                } else if (type.equals("losses") || type.equals("looses")) {
                    return getTopUUIDElement(plugin.getPlayerDataManager().getTopPlayersByLosses(3), pos, wantName);
                }
            }
        }

        if (params.startsWith("pos_")) {
            String type = params.replace("pos_", "");
            if (type.equals("souls")) {
                return String.valueOf(getPosString(plugin.getSoulsManager().getTopPlayers(1000), p.getName()));
            } else if (type.equals("games")) {
                return String
                        .valueOf(getPosUUID(plugin.getPlayerDataManager().getTopPlayersByGames(1000), p.getUniqueId()));
            } else if (type.equals("wins")) {
                return String
                        .valueOf(getPosUUID(plugin.getPlayerDataManager().getTopPlayersByWins(1000), p.getUniqueId()));
            } else if (type.equals("losses") || type.equals("looses")) {
                return String.valueOf(
                        getPosUUID(plugin.getPlayerDataManager().getTopPlayersByLosses(1000), p.getUniqueId()));
            }
        }

        return null;
    }

    private String getTopStringElement(java.util.Map<String, Integer> map, int pos, boolean wantName) {
        int i = 1;
        for (java.util.Map.Entry<String, Integer> entry : map.entrySet()) {
            if (i == pos) {
                return wantName ? entry.getKey() : String.valueOf(entry.getValue());
            }
            i++;
        }
        return wantName ? "Brak" : "0";
    }

    private String getTopUUIDElement(java.util.Map<java.util.UUID, Integer> map, int pos, boolean wantName) {
        int i = 1;
        for (java.util.Map.Entry<java.util.UUID, Integer> entry : map.entrySet()) {
            if (i == pos) {
                if (wantName) {
                    String n = org.bukkit.Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    return n == null ? "Nieznany" : n;
                } else {
                    return String.valueOf(entry.getValue());
                }
            }
            i++;
        }
        return wantName ? "Brak" : "0";
    }

    private int getPosString(java.util.Map<String, Integer> map, String name) {
        int i = 1;
        for (String k : map.keySet()) {
            if (k.equalsIgnoreCase(name))
                return i;
            i++;
        }
        return 0;
    }

    private int getPosUUID(java.util.Map<java.util.UUID, Integer> map, java.util.UUID uuid) {
        int i = 1;
        for (java.util.UUID k : map.keySet()) {
            if (k.equals(uuid))
                return i;
            i++;
        }
        return 0;
    }
}