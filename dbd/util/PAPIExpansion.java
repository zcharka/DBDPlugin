package pl.dbd.util;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

/**
 * <h1>PAPI EXPANSION – REFERENCJA PLACEHOLDERÓW DLA SKRIPT / AI AGENTÓW</h1>
 *
 * <p>Ta klasa rejestruje wszystkie placeholdery pod prefiksem <code>dbd</code>
 * (np. <code>%dbd_is_injured%</code>). Każdy placeholder jest dostępny
 * globalnie w Skripcie poprzez składnię:</p>
 *
 * <pre>{@code
 * # W pliku .sk używaj tak:
 * if "%placeholder api result of "dbd_is_injured" from player%" is "true":
 *     # gracz jest ranny
 * }</pre>
 *
 * <h2>LISTA PLACEHOLDERÓW BOOLOWSKICH (zwracają "true" / "false"):</h2>
 *
 * <h3>— Stany zdrowia gracza (PlayerState) —</h3>
 * <table>
 *   <tr><th>Placeholder</th><th>Opis</th></tr>
 *   <tr><td><code>%dbd_is_healthy%</code></td><td>Czy gracz jest zdrowy (HEALTHY)</td></tr>
 *   <tr><td><code>%dbd_is_injured%</code></td><td>Czy gracz jest ranny (INJURED)</td></tr>
 *   <tr><td><code>%dbd_is_downed%</code></td><td>Czy gracz leży/jest powalony (DOWNED)</td></tr>
 *   <tr><td><code>%dbd_is_hooked%</code></td><td>Czy gracz wisi na haku (HOOKED)</td></tr>
 *   <tr><td><code>%dbd_is_carried%</code></td><td>Czy gracz jest niesiony (CARRIED)</td></tr>
 *   <tr><td><code>%dbd_is_dead%</code></td><td>Czy gracz jest martwy (DEAD lub w deadPlayers)</td></tr>
 *   <tr><td><code>%dbd_is_in_locker%</code></td><td>Czy gracz jest schowany w szafce (IN_LOCKER)</td></tr>
 *   <tr><td><code>%dbd_is_escaped%</code></td><td>Czy gracz uciekł (escapedPlayers)</td></tr>
 * </table>
 *
 * <h3>— Rola gracza —</h3>
 * <table>
 *   <tr><td><code>%dbd_is_survivor%</code></td><td>Czy gracz jest Survivorem</td></tr>
 *   <tr><td><code>%dbd_is_killer%</code></td><td>Czy gracz jest Killerem</td></tr>
 *   <tr><td><code>%dbd_is_in_game%</code></td><td>Czy gracz uczestniczy w meczu (Killer LUB Survivor)</td></tr>
 * </table>
 *
 * <h3>— Stan meczu (globalny) —</h3>
 * <table>
 *   <tr><td><code>%dbd_game_is_lobby%</code></td><td>Czy mecz jest w stanie LOBBY</td></tr>
 *   <tr><td><code>%dbd_game_is_starting%</code></td><td>Czy mecz jest w stanie STARTING</td></tr>
 *   <tr><td><code>%dbd_game_is_in_game%</code></td><td>Czy mecz jest w stanie IN_GAME</td></tr>
 *   <tr><td><code>%dbd_game_is_ended%</code></td><td>Czy mecz jest w stanie ENDED</td></tr>
 * </table>
 *
 * <h3>— Inne istniejące (wartości tekstowe / liczbowe) —</h3>
 * <ul>
 *   <li><code>%dbd_souls%</code> / <code>%dbd_dusze%</code> — ilość dusz (liczba)</li>
 *   <li><code>%dbd_dusze_formatted%</code> — dusze sformatowane (np. "12.5k")</li>
 *   <li><code>%dbd_state%</code> / <code>%dbd_stan%</code> — stan zdrowia jako kolorowy tekst</li>
 *   <li><code>%dbd_role%</code> / <code>%dbd_rola%</code> — rola jako tekst ("Killer", "Survivor", "Lobby")</li>
 *   <li><code>%dbd_wins%</code>, <code>%dbd_losses%</code>, <code>%dbd_games%</code> — statystyki</li>
 * </ul>
 *
 * <h2>PRZYKŁAD UŻYCIA W SKRIPCIE:</h2>
 * <pre>{@code
 * # =====================================================
 * # SPRAWDZANIE STANU ZDROWIA GRACZA W SKRIPCIE
 * # =====================================================
 * #
 * # Placeholder: %dbd_is_injured%
 * # Jak zastosować: porównaj wynik z "true"
 * # Efekt: warunek jest spełniony gdy gracz ma stan INJURED
 * #
 * on damage of player:
 *     set {_injured} to placeholder "dbd_is_injured" from victim
 *     if {_injured} is "true":
 *         send "Gracz jest już ranny!" to attacker
 *
 * # =====================================================
 * # SPRAWDZANIE CZY MECZ TRWA
 * # =====================================================
 * #
 * # Placeholder: %dbd_game_is_in_game%
 * # Jak zastosować: porównaj wynik z "true"
 * # Efekt: warunek jest spełniony gdy mecz jest aktywny (IN_GAME)
 * #
 * every 1 second:
 *     set {_active} to placeholder "dbd_game_is_in_game" from player
 *     if {_active} is "true":
 *         # logika która działa tylko w trakcie meczu
 *
 * # =====================================================
 * # SPRAWDZANIE ROLI GRACZA
 * # =====================================================
 * #
 * # Placeholder: %dbd_is_killer%
 * # Jak zastosować: porównaj wynik z "true"
 * # Efekt: warunek jest spełniony gdy gracz jest Killerem
 * #
 * on right click:
 *     set {_isKiller} to placeholder "dbd_is_killer" from player
 *     if {_isKiller} is "true":
 *         send "Jesteś zabójcą!" to player
 * }</pre>
 */
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
        return "3.0";
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

        // ================================================================
        // BOOLEAN PLACEHOLDERS: STANY ZDROWIA (PlayerState)
        // Zwracają "true" lub "false".
        // Użycie w Skripcie:
        //   set {_val} to placeholder "dbd_is_injured" from player
        //   if {_val} is "true":
        // ================================================================

        if (lower.equals("is_healthy")) {
            return String.valueOf(plugin.getStateManager().getState(p) == PlayerStateManager.PlayerState.HEALTHY);
        }

        if (lower.equals("is_injured")) {
            return String.valueOf(plugin.getStateManager().isInjured(p));
        }

        if (lower.equals("is_downed")) {
            return String.valueOf(plugin.getStateManager().isDowned(p));
        }

        if (lower.equals("is_hooked")) {
            return String.valueOf(plugin.getStateManager().isHooked(p));
        }

        if (lower.equals("is_carried")) {
            return String.valueOf(plugin.getStateManager().isCarried(p));
        }

        if (lower.equals("is_dead")) {
            boolean dead = plugin.getStateManager().getState(p) == PlayerStateManager.PlayerState.DEAD
                    || plugin.getGameManager().isDead(p);
            return String.valueOf(dead);
        }

        if (lower.equals("is_in_locker")) {
            return String.valueOf(plugin.getStateManager().getState(p) == PlayerStateManager.PlayerState.IN_LOCKER);
        }

        if (lower.equals("is_escaped")) {
            return String.valueOf(plugin.getGameManager().hasEscaped(p));
        }

        // ================================================================
        // BOOLEAN PLACEHOLDERS: ROLA GRACZA
        // Zwracają "true" lub "false".
        // ================================================================

        if (lower.equals("is_survivor")) {
            return String.valueOf(plugin.getGameManager().isSurvivor(p));
        }

        if (lower.equals("is_killer")) {
            return String.valueOf(plugin.getGameManager().isKiller(p));
        }

        if (lower.equals("is_in_game")) {
            return String.valueOf(plugin.getGameManager().isInGame(p));
        }

        // ================================================================
        // BOOLEAN PLACEHOLDERS: STAN MECZU (globalny, nie per-gracz)
        // Zwracają "true" lub "false".
        // ================================================================

        if (lower.equals("game_is_lobby")) {
            return String.valueOf(
                    plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.LOBBY);
        }

        if (lower.equals("game_is_starting")) {
            return String.valueOf(
                    plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.STARTING);
        }

        if (lower.equals("game_is_in_game")) {
            return String.valueOf(
                    plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.IN_GAME);
        }

        if (lower.equals("game_is_ended")) {
            return String.valueOf(
                    plugin.getGameManager().getGameState() == pl.dbd.game.GameManager.GameState.ENDED);
        }

        // ================================================================
        // ISTNIEJĄCE PLACEHOLDERY (wartości tekstowe / liczbowe)
        // ================================================================

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

            if (plugin.getStateManager().getState(p) == PlayerStateManager.PlayerState.DEAD ||
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