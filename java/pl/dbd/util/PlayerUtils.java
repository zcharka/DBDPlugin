package pl.dbd.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PlayerUtils {

    public static void resetPlayerState(Player p) {
        // 1. Zrzucamy z niewidzialnego haka/pleców
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        
        // 2. Resetujemy lewitację i grawitację
        p.setGravity(true);
        p.setAllowFlight(false);
        p.setFlying(false);
        
        // 3. Usuwamy statusy i efekty
        p.removePotionEffect(PotionEffectType.LEVITATION);
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
        
        // 4. Czyszczenie ewentualnych zbugowanych armor standów w małym promieniu
        for (Entity e : p.getNearbyEntities(1.5, 1.5, 1.5)) {
            if (e.getType() == EntityType.ARMOR_STAND) {
                e.remove();
            }
        }
    }
}