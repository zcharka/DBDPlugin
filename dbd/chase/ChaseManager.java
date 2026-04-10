package pl.dbd.chase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class ChaseManager {
   private final DBDPlugin plugin;
   private final Map<UUID, ChaseManager.ChaseData> activeChases = new HashMap();

   public ChaseManager(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public void shutdown() {
      this.activeChases.clear();
   }

   public void resetAllChases() {
      this.activeChases.clear();
   }

   public boolean isInChase(Player player) {
      return this.activeChases.containsKey(player.getUniqueId());
   }

   public static class ChaseData {
   }
}
