package pl.dbd.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.dbd.DBDPlugin;
import pl.dbd.carry.CarrySystem; // POPRAWNY IMPORT

public class HitSystemListener implements Listener {
   private final DBDPlugin plugin;
   private final java.util.Map<java.util.UUID, Long> lastHitTime = new java.util.HashMap<>();

   public HitSystemListener(DBDPlugin plugin) {
      this.plugin = plugin;
   }

   public long getRemainingCooldownMs(java.util.UUID uuid) {
      Long last = lastHitTime.get(uuid);
      if (last == null)
         return 0;
      long cooldownMs = plugin.getConfig().getLong("killer-attack-cooldown-ms", 2000L);
      long passed = System.currentTimeMillis() - last;
      if (passed < cooldownMs) {
         return cooldownMs - passed;
      }
      return 0;
   }

   @EventHandler
   public void onSwing(org.bukkit.event.player.PlayerAnimationEvent e) {
      Player killer = e.getPlayer();
      if (!plugin.getGameManager().isInGame(killer))
         return;
      if (!plugin.getGameManager().isKiller(killer))
         return;

      long cooldownMs = plugin.getConfig().getLong("killer-attack-cooldown-ms", 2000L);
      long now = System.currentTimeMillis();
      java.util.UUID killerId = killer.getUniqueId();
      Long last = lastHitTime.get(killerId);

      if (last != null && (now - last) < cooldownMs) {
         return; // already on cooldown
      }

      lastHitTime.put(killerId, now);

      int slownessAmp = plugin.getConfig().getInt("killer-attack-slowness-amplifier", 3);
      int ticks = (int) (cooldownMs / 50L);
      if (slownessAmp >= 0 && ticks > 0) {
         killer.addPotionEffect(new org.bukkit.potion.PotionEffect(
               org.bukkit.potion.PotionEffectType.SLOWNESS, ticks, slownessAmp, false, false, true));
      }

      org.bukkit.inventory.ItemStack item = killer.getInventory().getItemInMainHand();
      if (item != null && item.getType() != org.bukkit.Material.AIR) {
         killer.setCooldown(item.getType(), ticks);
      }
   }

   @EventHandler
   public void onHit(EntityDamageByEntityEvent e) {
      if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player))
         return;

      Player killer = (Player) e.getDamager();
      Player victim = (Player) e.getEntity();

      // Nie pozwól na bicie graczy, którzy są w lobby / poza grą
      if (!plugin.getGameManager().isInGame(killer) || !plugin.getGameManager().isInGame(victim)) {
         e.setCancelled(true);
         return;
      }

      // Survivor CANNOT hit anyone
      if (plugin.getGameManager().isSurvivor(killer)) {
         e.setCancelled(true);
         return;
      }

      // Only killers can hit
      if (!plugin.getGameManager().isKiller(killer)) {
         e.setCancelled(true);
         return;
      }

      // Cooldown ataku: sprawdzamy czy minęło więcej niż 50ms od swingu
      // (bo AnimationEvent odpala się tuż przed DamageEvent)
      long cooldownMs = plugin.getConfig().getLong("killer-attack-cooldown-ms", 2000L);
      long now = System.currentTimeMillis();
      java.util.UUID killerId = killer.getUniqueId();
      Long last = lastHitTime.get(killerId);

      if (last != null && (now - last) < cooldownMs && (now - last) > 50) {
         e.setCancelled(true);
         return;
      }

      // Jeżeli to poprawne uderzenie z małym opóźnieniem (< 50ms), akceptujemy.
      lastHitTime.put(killerId, now);

      // Pobieramy CarrySystem z Maina
      CarrySystem carry = plugin.getCarrySystem();

      // Sprawdzamy, czy killer kogoś niesie (wtedy nie może bić)
      if (carry != null && carry.isCarrying(killer)) {
         e.setCancelled(true);
         return;
      }
      // Jeśli victim jest już powalony, na haku lub niesiony - nie dostaje hita
      pl.dbd.state.PlayerStateManager.PlayerState state = plugin.getStateManager().getState(victim);
      if (state == pl.dbd.state.PlayerStateManager.PlayerState.DOWNED ||
            state == pl.dbd.state.PlayerStateManager.PlayerState.HOOKED ||
            state == pl.dbd.state.PlayerStateManager.PlayerState.CARRIED ||
            state == pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
         e.setCancelled(true);
         return;
      }

      // Redukujemy prawdziwe obrażenia do zera (żeby gracz nie umarł z braku HP, ma
      // tylko zmieniać stany)
      e.setDamage(0);

      // Aplikujemy obrażenia i zmieniamy state ocalałego
      plugin.getStateManager().handleHit(victim, killer);
   }
}