package pl.dbd.protection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import pl.dbd.DBDPlugin;

public class UniversalProtectionListener implements Listener {
   private final DBDPlugin plugin;

   public UniversalProtectionListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   /**
    * Blokada tracenia HP (minecraftowego) – wszystkie obrażenia graczy anulowane
    * z wyjątkiem ciosów gracz->gracz,
    * które obsługuje HitSystemListener (tam damage jest ustawiany na 0).
    */
   @EventHandler(priority = EventPriority.LOW)
   public void onDamage(EntityDamageEvent event) {
      if (!(event.getEntity() instanceof Player))
         return;

      // Jeśli obrażenia zadał gracz – zostawiamy event dla HitSystemListener
      if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntity
            && byEntity.getDamager() instanceof Player) {
         return;
      }

      // Inne źródła obrażeń (upadek, ogień, moby, itp.) kasujemy
      event.setCancelled(true);
   }

   /**
    * Blokada palenia się ogólnie (nie tylko w meczu) – ogień, lawa, podpalenie.
    */
   @EventHandler(priority = EventPriority.LOW)
   public void onCombust(EntityCombustEvent event) {
      event.setCancelled(true);
   }

   @EventHandler(priority = EventPriority.LOWEST)
   public void onCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
      Player p = event.getPlayer();
      pl.dbd.game.GameManager gm = plugin.getGameManager();

      if (gm == null)
         return;

      boolean inGame = gm.isInGame(p);
      boolean inLobby = gm.getReadyPlayers().contains(p.getUniqueId())
            || gm.getQueuedPlayers().contains(p.getUniqueId());

      if (inGame || inLobby) {
         String message = event.getMessage().toLowerCase();
         String[] args = message.split(" ");
         String command = args[0];

         java.util.List<String> blockedCommands = new java.util.ArrayList<>();
         if (inGame) {
            blockedCommands.addAll(plugin.getConfig().getStringList("blocked-game-commands"));
         }
         if (inLobby) {
            blockedCommands.addAll(plugin.getConfig().getStringList("blocked-lobby-commands"));
         }

         for (String blocked : blockedCommands) {
            // allows /gotowy, /opusc to work even if someone misconfigures it
            if (command.equalsIgnoreCase("/opusc") || command.equalsIgnoreCase("/gotowy")
                  || command.equalsIgnoreCase("/niegotowy")) {
               continue;
            }
            if (command.equalsIgnoreCase(blocked)) {
               event.setCancelled(true);
               if (inGame) {
                  p.sendMessage("§cNie możesz używać tej komendy podczas trwania meczu!");
               } else {
                  p.sendMessage(
                        "§cNie możesz używać tej komendy podczas oczekiwania na grę! Wpisz /opusc aby wyjść z kolejki.");
               }
               return;
            }
         }
      }
   }
}
