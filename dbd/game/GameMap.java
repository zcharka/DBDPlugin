package pl.dbd.game;

import org.bukkit.Location;

public class GameMap {
   private final String name;
   private final Location survivorSpawn;
   private final Location killerSpawn;

   public GameMap(String name, Location survivorSpawn, Location killerSpawn) {
      this.name = name;
      this.survivorSpawn = survivorSpawn;
      this.killerSpawn = killerSpawn;
   }

   public String getName() {
      return this.name;
   }

   public Location getSurvivorSpawn() {
      return this.survivorSpawn != null ? this.survivorSpawn.clone() : null;
   }

   public Location getKillerSpawn() {
      return this.killerSpawn != null ? this.killerSpawn.clone() : null;
   }
}
