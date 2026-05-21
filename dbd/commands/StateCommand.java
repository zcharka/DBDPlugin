package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

/**
 * <h1>KOMENDA DO USTAWIANIA STANÓW GRACZA Z POZIOMU SKRIPTU / KONSOLI</h1>
 *
 * <p>Komenda: <code>/dbdstate &lt;gracz&gt; &lt;stan&gt;</code></p>
 *
 * <p>Dostępne stany:</p>
 * <ul>
 *   <li><code>downed</code> — powalony (gracz leży/czołga się)</li>
 *   <li><code>injured</code> — ranny</li>
 *   <li><code>healthy</code> — zdrowy (pełne leczenie)</li>
 *   <li><code>dead</code> — martwy</li>
 *   <li><code>hooked</code> — na haku</li>
 *   <li><code>carried</code> — niesiony</li>
 *   <li><code>in_locker</code> — w szafce</li>
 * </ul>
 *
 * <h2>UŻYCIE W SKRIPCIE:</h2>
 * <pre>{@code
 * # Powalenie gracza (leżenie):
 * execute console command "dbdstate %player% downed"
 *
 * # Sprawdzenie czy leży:
 * set {_isDowned} to placeholder "dbd_is_downed" from player
 * if {_isDowned} is "true":
 *     # gracz leży
 *
 * # Podniesienie gracza (uleczenie):
 * execute console command "dbdstate %player% healthy"
 * }</pre>
 */
public class StateCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public StateCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            sender.sendMessage("§cBrak uprawnień!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /dbdstate <gracz> <stan>");
            sender.sendMessage("§7Dostępne stany: §edowned§7, §einjured§7, §ehealthy§7, §edead§7, §ehooked§7, §ecarried§7, §ein_locker");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cNie znaleziono gracza: " + args[0]);
            return true;
        }

        String stateName = args[1].toLowerCase();
        PlayerStateManager psm = plugin.getStateManager();

        switch (stateName) {
            case "downed":
            case "down":
            case "powalony":
                // Używamy setDowned() — prawidłowo ustawia stan DOWNED
                // oraz aplikuje animację czołgania (applyDownedState)
                psm.setDowned(target.getUniqueId());
                sender.sendMessage("§aGracz §e" + target.getName() + " §azostał powalony (DOWNED) — leży na ziemi.");
                break;

            case "injured":
            case "ranny":
                psm.setState(target, PlayerStateManager.PlayerState.INJURED);
                sender.sendMessage("§aGracz §e" + target.getName() + " §azostał ranny (INJURED).");
                break;

            case "healthy":
            case "heal":
            case "zdrowy":
                psm.heal(target);
                sender.sendMessage("§aGracz §e" + target.getName() + " §azostał uleczony (HEALTHY).");
                break;

            case "dead":
            case "martwy":
                psm.setState(target, PlayerStateManager.PlayerState.DEAD);
                sender.sendMessage("§aGracz §e" + target.getName() + " §aoznaczony jako martwy (DEAD).");
                break;

            case "hooked":
            case "hak":
                psm.setState(target, PlayerStateManager.PlayerState.HOOKED);
                sender.sendMessage("§aGracz §e" + target.getName() + " §ana haku (HOOKED).");
                break;

            case "carried":
            case "niesiony":
                psm.setState(target, PlayerStateManager.PlayerState.CARRIED);
                sender.sendMessage("§aGracz §e" + target.getName() + " §ajest niesiony (CARRIED).");
                break;

            case "in_locker":
            case "locker":
            case "szafa":
                psm.setState(target, PlayerStateManager.PlayerState.IN_LOCKER);
                sender.sendMessage("§aGracz §e" + target.getName() + " §aw szafce (IN_LOCKER).");
                break;

            default:
                sender.sendMessage("§cNieznany stan: §e" + stateName);
                sender.sendMessage("§7Dostępne: §edowned§7, §einjured§7, §ehealthy§7, §edead§7, §ehooked§7, §ecarried§7, §ein_locker");
                return true;
        }

        return true;
    }
}
