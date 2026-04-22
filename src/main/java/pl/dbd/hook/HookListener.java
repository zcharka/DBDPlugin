package pl.dbd.hook;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.dbd.DBDPlugin;
import java.util.UUID;

public class HookListener implements Listener {
    private final DBDPlugin plugin;

    public HookListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType() == Material.TRIPWIRE_HOOK) {
                 Hook h = plugin.getHookManager().getHookAt(e.getClickedBlock().getLocation());
                 if (h != null && h.isOccupied()) {
                     plugin.getHookManager().unhookPlayer(h.getHookedPlayer());
                 }
            }
        }
    }

    // --- METODY WYMAGANE PRZEZ GameManager i SkillCheckGUI ---

    public void cleanupAllHooks() {
        plugin.getHookManager().resetAllHooks();
    }
    
    public void resetPlayerHookCount(UUID uuid) {
        // Tu można zresetować licznik powieszeń w HookManagerze
        // Na razie puste, żeby się kompilowało, jeśli nie masz logiki etapów
    }

    public void forceStage3Death(Player p) {
        if (p != null) {
            p.setHealth(0);
            plugin.getGameManager().markDead(p);
            plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.DEAD);
            plugin.getHookManager().unhookPlayer(p); // Zdejmij wizualnie
        }
    }
}