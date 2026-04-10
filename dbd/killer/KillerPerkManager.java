package pl.dbd.killer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

public class KillerPerkManager {
   private final DBDPlugin plugin;
   private BukkitRunnable perkTask;
   private final Map<UUID, Integer> activePerkLevels;

   public KillerPerkManager(DBDPlugin plugin) {
      this.plugin = plugin;
      this.activePerkLevels = new HashMap();
      this.startPerkDetection();
   }

   private void startPerkDetection() {
      this.perkTask = new BukkitRunnable() {
         public void run() {
            GameManager gameManager = KillerPerkManager.this.plugin.getGameManager();
            if (gameManager != null && gameManager.getGameState() == GameManager.GameState.IN_GAME) {
               Iterator var2 = Bukkit.getOnlinePlayers().iterator();

               while (var2.hasNext()) {
                  Player killer = (Player) var2.next();
                  if (gameManager.isKiller(killer)) {
                     KillerPerkManager.this.updateKillerPerks(killer, gameManager);
                  }
               }
            }

         }
      };
      this.perkTask.runTaskTimer(this.plugin, 0L, 10L);
   }

   private void updateKillerPerks(Player killer, GameManager gameManager) {
      int perkLevel = this.getSokolePerkLevel(killer);
      this.activePerkLevels.put(killer.getUniqueId(), perkLevel);
      if (perkLevel != 0) {
         double range = this.getPerkRange(perkLevel);
         Iterator var6 = Bukkit.getOnlinePlayers().iterator();

         while (var6.hasNext()) {
            Player survivor = (Player) var6.next();
            if (gameManager.isSurvivor(survivor) && !gameManager.isDead(survivor)
                  && !gameManager.hasEscaped(survivor)) {
               if (killer.getWorld().equals(survivor.getWorld())
                     && killer.getLocation().distance(survivor.getLocation()) <= range) {
                  this.applyGlowEffect(survivor, killer);
               } else {
                  this.removeGlowEffect(survivor, killer);
               }
            }
         }
      }

   }

   private int getSokolePerkLevel(Player killer) {
      ItemStack[] inventory = killer.getInventory().getContents();
      ItemStack[] var3 = inventory;
      int var4 = inventory.length;

      for (int var5 = 0; var5 < var4; ++var5) {
         ItemStack item = var3[var5];
         if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            if (displayName.contains("ꜱᴏᴋᴏʟᴀ ᴋʀᴇᴡ III") && item.getType() == Material.PURPLE_DYE) {
               return 3;
            }

            if (displayName.contains("ꜱᴏᴋᴏʟᴀ ᴋʀᴇᴡ II") && item.getType() == Material.BLUE_DYE) {
               return 2;
            }

            if (displayName.contains("ꜱᴏᴋᴏʟᴀ ᴋʀᴇᴡ I") && item.getType() == Material.LIME_DYE) {
               return 1;
            }
         }
      }

      return 0;
   }

   private double getPerkRange(int level) {
      switch (level) {
         case 1:
            return 3.0D;
         case 2:
            return 6.0D;
         case 3:
            return 8.0D;
         default:
            return 0.0D;
      }
   }

   private void applyGlowEffect(Player survivor, Player killer) {
      if (!survivor.hasPotionEffect(PotionEffectType.GLOWING)) {
         survivor.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false, false, false));
      }

   }

   private void removeGlowEffect(Player survivor, Player killer) {
      GameManager gameManager = this.plugin.getGameManager();
      if (gameManager != null) {
         boolean shouldGlow = false;
         Iterator var5 = Bukkit.getOnlinePlayers().iterator();

         while (var5.hasNext()) {
            Player otherKiller = (Player) var5.next();
            if (gameManager.isKiller(otherKiller) && !otherKiller.equals(killer)) {
               int perkLevel = (Integer) this.activePerkLevels.getOrDefault(otherKiller.getUniqueId(), 0);
               if (perkLevel > 0) {
                  double range = this.getPerkRange(perkLevel);
                  if (otherKiller.getWorld().equals(survivor.getWorld())
                        && otherKiller.getLocation().distance(survivor.getLocation()) <= range) {
                     shouldGlow = true;
                     break;
                  }
               }
            }
         }

         if (!shouldGlow) {
            survivor.removePotionEffect(PotionEffectType.GLOWING);
         }
      }

   }

   public void shutdown() {
      if (this.perkTask != null) {
         this.perkTask.cancel();
      }

      this.activePerkLevels.clear();
      Iterator var1 = Bukkit.getOnlinePlayers().iterator();

      while (var1.hasNext()) {
         Player player = (Player) var1.next();
         player.removePotionEffect(PotionEffectType.GLOWING);
      }

   }

   public void resetPerks() {
      this.activePerkLevels.clear();
      Iterator var1 = Bukkit.getOnlinePlayers().iterator();

      while (var1.hasNext()) {
         Player player = (Player) var1.next();
         player.removePotionEffect(PotionEffectType.GLOWING);
      }

   }
}
