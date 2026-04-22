package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

public class Generator {
    private final Location location;
    private ItemDisplay itemDisplay; // Opcjonalne, jeśli używamy
    private double progress = 0.0;
    private boolean isCompleted = false;
    private boolean isRepairing = false;

    public Generator(Location location, ItemDisplay itemDisplay) {
        this.location = location;
        this.itemDisplay = itemDisplay;
    }

    public Location getLocation() { return location; }
    public ItemDisplay getItemDisplay() { return itemDisplay; }
    public void setItemDisplay(ItemDisplay display) { this.itemDisplay = display; }
    
    public double getProgress() { return progress; }
    public void setProgress(double p) { this.progress = p; }
    
    public boolean isCompleted() { return isCompleted; }
    
    // TEJ METODY BRAKOWAŁO:
    public void setCompleted(boolean completed) { 
        this.isCompleted = completed; 
        if (completed) {
            this.progress = 100.0;
            // Efekt naprawienia
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.0f);
        }
    }

    public void addProgress(double amount) {
        if (isCompleted) return;
        this.progress += amount;
        if (this.progress >= 100.0) {
            this.progress = 100.0;
            setCompleted(true);
            // Informacja globalna
            Bukkit.broadcastMessage("§aGenerator został naprawiony!");
        }
    }

    public void failSkillCheck() {
        this.progress -= 10.0;
        if (this.progress < 0) this.progress = 0;
    }

    public void notifyGeneratorInterrupted() {
        // Logika wybuchu, powiadomienia killera (obsługiwana też w GUI/Listenerze)
        location.getWorld().createExplosion(location, 0F, false);
    }
}