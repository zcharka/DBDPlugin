package pl.dbd.placeholder;

import pl.dbd.stats.PlayerStatsManager;

/** @deprecated */
@Deprecated
public class StatsPlaceholder {
   private final PlayerStatsManager statsManager;

   public StatsPlaceholder(PlayerStatsManager statsManager) {
      this.statsManager = statsManager;
   }
}
