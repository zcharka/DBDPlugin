package pl.dbd.state;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateManager {
    private final DBDPlugin plugin;
    private final Map<UUID, PlayerState> states = new HashMap<>();

    public PlayerStateManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public enum PlayerState {
        HEALTHY, INJURED, DOWNED, HOOKED, CARRIED, DEAD
    }

    public PlayerState getState(Player p) {
        return states.getOrDefault(p.getUniqueId(), PlayerState.HEALTHY);
    }

    public void setState(Player p, PlayerState state) {
        states.put(p.getUniqueId(), state);
        // Możesz tu dodać logikę efektów wizualnych dla zmiany stanu
    }

    // --- METODY WYMAGANE PRZEZ INNE PLIKI (NAPRAWA BŁĘDÓW) ---

    public boolean isDowned(Player p) {
        return getState(p) == PlayerState.DOWNED;
    }

    public boolean isInjured(Player p) {
        return getState(p) == PlayerState.INJURED;
    }

    public boolean isCarried(Player p) {
        return getState(p) == PlayerState.CARRIED;
    }

    public boolean isHooked(Player p) {
        return getState(p) == PlayerState.HOOKED;
    }

    public void setDowned(UUID uuid) {
        Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null) {
            setState(p, PlayerState.DOWNED);
            applyDownedState(p);
        }
    }

    public void setInjured(UUID uuid) {
        Player p = org.bukkit.Bukkit.getPlayer(uuid);
        if (p != null) setState(p, PlayerState.INJURED);
    }

    public void applyDownedState(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 3, false, false));
        p.setSwimming(true);
    }
    
    public void heal(Player p) {
        setState(p, PlayerState.HEALTHY);
        p.setSwimming(false);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
    }
}