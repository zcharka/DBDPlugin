package pl.dbd.listeners;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import pl.dbd.DBDPlugin;
// WAŻNE: Poprawiony import (EqEntry jest teraz osobnym plikiem)
import pl.dbd.equipment.EqEntry;

public class EquipmentListener implements Listener {
    private final DBDPlugin plugin;

    public EquipmentListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        String itemMat = e.getItem().getType().name();

        // 1. Logika LATARKI
        EqEntry flashEntry = plugin.getEquipmentManager().getEntry("latarka");
        if (flashEntry != null && itemMat.equals(flashEntry.getMaterial().name())) {
            e.setCancelled(true);
            handleFlashlight(p);
            return;
        }

        // 2. Logika APTECZKI
        EqEntry medEntry = plugin.getEquipmentManager().getEntry("apteczka");
        if (medEntry != null && itemMat.equals(medEntry.getMaterial().name())) {
            e.setCancelled(true);
            handleMedkit(p);
        }
    }

    private void handleFlashlight(Player p) {
        if (p.hasCooldown(p.getInventory().getItemInMainHand().getType())) return;
        
        p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 2f);

        RayTraceResult result = p.getWorld().rayTraceEntities(
            p.getEyeLocation(), 
            p.getEyeLocation().getDirection(), 
            10.0, 
            ent -> ent instanceof Player && !ent.getUniqueId().equals(p.getUniqueId())
        );

        if (result != null && result.getHitEntity() instanceof Player) {
            Player target = (Player) result.getHitEntity();
            
            // Sprawdzamy czy to killer (zakładamy, że GameManager ma tę metodę)
            if (plugin.getGameManager().isKiller(target)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                
                target.sendMessage("§cZostałeś oślepiony!");
                p.sendMessage("§aOślepiłeś killera!");
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                
                plugin.getCarrySystem().dropSurvivor(target);
            }
        }
        p.setCooldown(p.getInventory().getItemInMainHand().getType(), 40);
    }

    private void handleMedkit(Player p) {
        if (plugin.getStateManager().isInjured(p)) {
            p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);
            plugin.getStateManager().heal(p);
            p.sendMessage("§aUleczono!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        } else {
            p.sendMessage("§cJesteś w pełni zdrowy.");
        }
    }
}