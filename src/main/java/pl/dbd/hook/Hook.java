package pl.dbd.hook;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Hook {
    private final Location location;
    private Player hookedPlayer;
    private int hookStage = 0; // 0 = pusty, 1 = wiszenie, 2 = struggle

    public Hook(Location loc) {
        this.location = loc;
    }
    
    public void hookPlayer(Player p) {
        this.hookedPlayer = p;
        this.hookStage = 1;
        p.teleport(location.clone().add(0.5, 1.5, 0.5));
    }
    
    // Tę metodę wywołujesz w HookManagerze bez argumentów
    public void unhookPlayer() {
        this.hookedPlayer = null;
        this.hookStage = 0;
    }
    
    // Metody, o które krzyczał kompilator:
    public boolean isOccupied() {
        return hookedPlayer != null;
    }

    public int getHookStage() {
        return hookStage;
    }

    public void setHookStage(int stage) {
        this.hookStage = stage;
    }
    
    public Player getHookedPlayer() { return hookedPlayer; }
    public Location getLocation() { return location; }
    public void setHookedPlayer(Player p) { this.hookedPlayer = p; }
}