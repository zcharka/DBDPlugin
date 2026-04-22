package pl.dbd.exitgate;

import org.bukkit.Location;

public class ExitGate {
   private final Location location;
   private boolean opened = false;
   private double progress = 0.0D;

   public ExitGate(Location location) {
      this.location = location;
   }

   public Location getLocation() {
      return this.location.clone();
   }

   public boolean isOpened() {
      return this.opened;
   }

   public double getProgress() {
      return this.progress;
   }

   public void addProgress(double amount) {
      this.progress = Math.min(100.0D, this.progress + amount);
      if (this.progress >= 100.0D) {
         this.opened = true;
      }

   }

   public void open() {
      this.opened = true;
      this.progress = 100.0D;
   }

   public void reset() {
      this.opened = false;
      this.progress = 0.0D;
   }
}
