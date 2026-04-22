package pl.dbd.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.dbd.DBDPlugin;
import pl.dbd.currency.BloodPointsManager;

public class BloodPointsPlaceholder extends PlaceholderExpansion {
   private final DBDPlugin plugin;
   private final BloodPointsManager bloodPointsManager;

   public BloodPointsPlaceholder(DBDPlugin plugin, BloodPointsManager bloodPointsManager) {
      this.plugin = plugin;
      this.bloodPointsManager = bloodPointsManager;
   }

   @NotNull
   public String getIdentifier() {
      return "dbd";
   }

   @NotNull
   public String getAuthor() {
      return this.plugin.getDescription().getAuthors().toString();
   }

   @NotNull
   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onPlaceholderRequest(Player player, @NotNull String params) {
      if (player == null) {
         return "";
      } else if (params.equals("bloodpoints")) {
         return String.valueOf(this.bloodPointsManager.getBloodPoints(player));
      } else if (params.equals("bloodpoints_formatted")) {
         int amount = this.bloodPointsManager.getBloodPoints(player);
         return this.formatNumber(amount);
      } else {
         return null;
      }
   }

   private String formatNumber(int number) {
      return String.format("%,d", number);
   }
}
