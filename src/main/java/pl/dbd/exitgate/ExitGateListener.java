package pl.dbd.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.dbd.DBDPlugin;

public class ExitGateListener implements Listener {
    private final DBDPlugin plugin;

    public ExitGateListener(DBDPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && b.getType() == Material.IRON_DOOR) {
                // Pozwalamy otworzyć, jeśli to Survivor
                if (plugin.getGameManager().isSurvivor(e.getPlayer())) {
                    Openable door = (Openable) b.getBlockData();
                    if (!door.isOpen()) {
                        door.setOpen(true);
                        b.setBlockData(door);
                        e.getPlayer().sendMessage("§aOtwierasz bramę wyjściową!");
                    }
                }
            }
        }
    }
}