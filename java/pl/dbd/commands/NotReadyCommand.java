package pl.dbd.commands;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class NotReadyCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public NotReadyCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player p = (Player) sender;

        if (plugin.getGameManager().getGameState() != pl.dbd.game.GameManager.GameState.LOBBY) {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("lobby-busy-not-ready"));
            return true;
        }

        if (plugin.getGameManager().getReadyPlayers().contains(p.getUniqueId())) {
            plugin.getGameManager().leaveLobby(p);
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("copucielobbyikolejk"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);

            // Opcjonalnie teleport na spawn świata, żeby nie stał w lobby
            Location worldSpawn = p.getWorld().getSpawnLocation();
            if (worldSpawn != null)
                p.teleport(worldSpawn);
        } else {
            p.sendMessage(pl.dbd.DBDPlugin.getMsg("cniejestewkolejce"));
        }

        return true;
    }
}