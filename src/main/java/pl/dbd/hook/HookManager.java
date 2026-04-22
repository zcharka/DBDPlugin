package pl.dbd.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HookManager {
    private final DBDPlugin plugin;
    private final List<Hook> hooks = new ArrayList<>();

    public HookManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public void createHook(Location loc) {
        hooks.add(new Hook(loc));
    }

    public Hook getHookAt(Location loc) {
        for (Hook h : hooks) {
            // Sprawdzamy dystans, żeby wykryć hak
            if (h.getLocation().distance(loc) < 1.5) return h;
        }
        return null;
    }
    
    // TEJ METODY BRAKOWAŁO (dla komendy remove)
    public boolean removeHook(Location loc) {
        Hook h = getHookAt(loc);
        if (h != null) {
            hooks.remove(h);
            return true;
        }
        return false;
    }
    
    // TEJ METODY BRAKOWAŁO (dla komendy debug)
    public void resetAllHooks() {
        for (Hook h : hooks) {
            h.unhookPlayer();
        }
    }
    
    // TEJ METODY BRAKOWAŁO (dla komendy list)
    public List<Hook> getAllHooks() {
        return new ArrayList<>(hooks);
    }

    // TEJ METODY BRAKOWAŁO (dla SkillCheckListenera)
    public boolean isPlayerHooked(UUID uuid) {
        for (Hook h : hooks) {
            if (h.getHookedPlayer() != null && h.getHookedPlayer().getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void hookPlayer(Player p, Hook hook) {
        if (hook.isOccupied()) return;
        hook.hookPlayer(p);
        plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.HOOKED);
        p.sendMessage("§cZostałeś powieszony na haku!");
    }

    public void unhookPlayer(Player p) {
        for (Hook h : hooks) {
            if (h.getHookedPlayer() != null && h.getHookedPlayer().getUniqueId().equals(p.getUniqueId())) {
                h.unhookPlayer();
                plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.INJURED);
                p.sendMessage("§aZostałeś zdjęty z haka!");
                return;
            }
        }
    }
}