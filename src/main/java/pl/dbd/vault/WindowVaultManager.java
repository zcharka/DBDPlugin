package pl.dbd.vault;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;

public class WindowVaultManager {
   private final Set<Location> blockedWindows = new HashSet();

   public boolean isBlocked(Location loc) {
      return this.blockedWindows.contains(loc);
   }

   public void block(Location loc) {
      this.blockedWindows.add(loc);
   }

   public void unblock(Location loc) {
      this.blockedWindows.remove(loc);
   }

   public boolean has(Location loc) {
      return this.blockedWindows.contains(loc);
   }

   public void clear() {
      this.blockedWindows.clear();
   }
}
