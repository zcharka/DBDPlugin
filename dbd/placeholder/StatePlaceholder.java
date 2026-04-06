package pl.dbd.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import pl.dbd.state.PlayerStateManager;

public class StatePlaceholder extends PlaceholderExpansion {
   private final PlayerStateManager stateManager;

   public StatePlaceholder(PlayerStateManager stateManager) {
      this.stateManager = stateManager;
   }

   public String getIdentifier() {
      return "dbd";
   }

   public String getAuthor() {
      return "DBD";
   }

   public String getVersion() {
      return "1.0";
   }

   public boolean persist() {
      return true;
   }

   public String onPlaceholderRequest(Player player, String params) {
      if (player == null) {
         return "";
      } else if (params.equalsIgnoreCase("state")) {
         if (pl.dbd.DBDPlugin.getInstance().getGameManager() != null
               && !pl.dbd.DBDPlugin.getInstance().getGameManager().isSurvivor(player)) {
            return "§cZABÓJCA"; // Możesz zmienić na "" jeśli ma być puste
         }
         PlayerStateManager.PlayerState state = this.stateManager.getState(player);
         return this.translateState(state);
      } else if (params.equalsIgnoreCase("downed")) {
         return this.stateManager.isDowned(player) ? "TAK" : "NIE";
      } else if (params.equalsIgnoreCase("injured")) {
         return this.stateManager.isInjured(player) ? "TAK" : "NIE";
      } else if (params.equalsIgnoreCase("carried")) {
         return this.stateManager.isCarried(player) ? "TAK" : "NIE";
      } else if (params.equalsIgnoreCase("hooked")) {
         return this.stateManager.isHooked(player) ? "TAK" : "NIE";
      } else {
         return null;
      }
   }

   private String translateState(PlayerStateManager.PlayerState state) {
      switch (state) {
         case HEALTHY:
            return "§aZDROWY";
         case INJURED:
            return "§cRANNY";
         case DOWNED:
            return "§cLEŻY";

         case CARRIED:
            return "§4NIESIONY";
         case HOOKED:
            return "§4NA HAKU";
         case DEAD:
            return "§8MARTWY";
         case IN_LOCKER:
            return "§9W SZAFIE";
         default:
            return "§7" + state.toString();
      }
   }
}
