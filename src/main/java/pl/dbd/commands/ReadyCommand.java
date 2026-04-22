package pl.dbd.commands;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

public class ReadyCommand implements CommandExecutor {

    private final DBDPlugin plugin;

    public ReadyCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        GameManager gm = plugin.getGameManager();

        // 1. Sprawdź czy lobby jest ustawione
        if (gm.getLobbySpawn() == null) {
            p.sendMessage("§cLobby gry nie jest ustawione! Admin musi wpisać: §e/game setlobby");
            return true;
        }

        // 2. Sprawdź czy gra już nie trwa
        if (gm.getGameState() != GameManager.GameState.LOBBY) {
            p.sendMessage("§cGra już trwa!");
            return true;
        }

        // 3. Logika dołączania
        if (gm.getReadyPlayers().contains(p.getUniqueId())) {
            p.sendMessage("§eJuż jesteś w kolejce!");
            return true;
        }

        // Dodajemy do lobby
        gm.joinLobby(p);
        p.teleport(gm.getLobbySpawn());
        
        // 4. Potwierdzenie
        p.sendMessage("§a§lDołączyłeś do lobby!");
        p.sendMessage("§7Oczekiwanie na graczy: §e" + gm.getReadyPlayers().size() + "/5");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

        return true;
    }
}