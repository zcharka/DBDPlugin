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
      } else if (params.equals("dusze")) {
         return String.valueOf(this.plugin.getSoulsManager().getBalance(player));
      } else if (params.equals("dusze_formatted")) {
         return formatShort(this.plugin.getSoulsManager().getBalance(player));
      } else {
         return null;
      }
   }

   private String formatNumber(int number) {
      return String.format("%,d", number);
   }

   private String formatShort(long value) {
      if (value < 0)
         return "-" + formatShort(-value);
      if (value < 1000)
         return String.valueOf(value);
      if (value < 1_000_000) {
         double v = value / 1000.0;
         return (v == (long) v) ? (long) v + "k" : String.format("%.1fk", v);
      }
      if (value < 1_000_000_000) {
         double v = value / 1_000_000.0;
         return (v == (long) v) ? (long) v + "M" : String.format("%.1fM", v);
      }
      double v = value / 1_000_000_000.0;
      return (v == (long) v) ? (long) v + "B" : String.format("%.1fB", v);
   }
}
