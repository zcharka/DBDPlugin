package pl.dbd.exitgate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import pl.dbd.DBDPlugin;

public class ExitGateManager {
   private final DBDPlugin plugin;
   private final Map<String, ExitGate> exitGates = new HashMap();

   public ExitGateManager(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   private String key(Location loc) {
      String var10000 = loc.getWorld().getName();
      return var10000 + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
   }

   public void createExitGate(Location loc) {
      Location bLoc = loc.getBlock().getLocation();
      this.exitGates.put(this.key(bLoc), new ExitGate(bLoc));
   }

   public void removeExitGate(Location loc) {
      this.exitGates.remove(this.key(loc));
   }

   public ExitGate getExitGateAt(Location loc) {
      return (ExitGate)this.exitGates.get(this.key(loc));
   }

   public Collection<ExitGate> getAllExitGates() {
      return this.exitGates.values();
   }

   public Collection<ExitGate> getAllGates() {
      return this.exitGates.values();
   }

   public int getOpenCount() {
      return (int)this.exitGates.values().stream().filter(ExitGate::isOpened).count();
   }

   public void resetAllGates() {
      this.exitGates.values().forEach(ExitGate::reset);
   }

   public void openExitGate(ExitGate gate) {
      gate.open();
   }

   public void saveGates() {
   }
}
