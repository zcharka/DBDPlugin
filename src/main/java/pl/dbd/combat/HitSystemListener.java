package pl.dbd.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.dbd.DBDPlugin;
import pl.dbd.carry.CarrySystem; // POPRAWNY IMPORT

public class HitSystemListener implements Listener {
    private final DBDPlugin plugin;

    public HitSystemListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) return;
        
        Player killer = (Player) e.getDamager();
        
        // Pobieramy CarrySystem z Maina
        CarrySystem carry = plugin.getCarrySystem();
        
        // Sprawdzamy, czy killer kogoś niesie (wtedy nie może bić)
        if (carry != null && carry.isCarrying(killer)) {
            e.setCancelled(true);
            return;
        }
        
        // Tutaj powinna być dalsza logika hita (np. downowanie gracza)
        // Jeśli masz to w innej klasie (np. DownManager), to zostaw.
        // Poniżej przykładowe wywołanie (odkomentuj jeśli masz taką metodę):
        // plugin.getStateManager().handleHit((Player)e.getEntity());
    }
}