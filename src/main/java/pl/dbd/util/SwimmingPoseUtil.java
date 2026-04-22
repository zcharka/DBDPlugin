package pl.dbd.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class SwimmingPoseUtil {
   private static boolean nmsAvailable = true;

   public static void forceSwimming(Player player) {
      if (player.isOnline()) {
         if (nmsAvailable) {
            try {
               forceViaNMS(player);
               return;
            } catch (Exception var2) {
               nmsAvailable = false;
            }
         }

         player.removePotionEffect(PotionEffectType.JUMP_BOOST);
         player.setSwimming(true);
      }
   }

   public static void clearSwimming(Player player) {
      if (player.isOnline()) {
         player.setSwimming(false);
      }
   }

   private static void forceViaNMS(Player player) throws Exception {
      Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);

      Class entityClass;
      for(entityClass = nmsPlayer.getClass().getSuperclass(); entityClass != null && !entityClass.getSimpleName().equals("Entity"); entityClass = entityClass.getSuperclass()) {
      }

      if (entityClass == null) {
         throw new Exception("Entity class not found");
      } else {
         Field dataField = null;
         Field[] var4 = entityClass.getDeclaredFields();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Field f = var4[var6];
            if (f.getType().getSimpleName().contains("SynchedEntityData") || f.getType().getSimpleName().contains("EntityDataManager") || f.getType().getSimpleName().contains("DataWatcher")) {
               dataField = f;
               break;
            }
         }

         if (dataField == null) {
            throw new Exception("EntityData field not found");
         } else {
            dataField.setAccessible(true);
            Object synchedData = dataField.get(nmsPlayer);
            Field flagsAccessorField = null;
            Field[] var13 = entityClass.getDeclaredFields();
            int var15 = var13.length;

            for(int var8 = 0; var8 < var15; ++var8) {
               Field f = var13[var8];
               if (f.getType().getSimpleName().contains("EntityDataAccessor") || f.getType().getSimpleName().contains("DataWatcherObject")) {
                  flagsAccessorField = f;
                  break;
               }
            }

            if (flagsAccessorField == null) {
               throw new Exception("Flags accessor not found");
            } else {
               flagsAccessorField.setAccessible(true);
               Object flagsAccessor = flagsAccessorField.get((Object)null);
               Method getMethod = synchedData.getClass().getMethod("get", flagsAccessor.getClass().getInterfaces()[0]);
               byte current = (Byte)getMethod.invoke(synchedData, flagsAccessor);
               byte newFlags = (byte)(current | 16);
               Method setMethod = synchedData.getClass().getMethod("set", flagsAccessor.getClass().getInterfaces()[0], Object.class);
               setMethod.invoke(synchedData, flagsAccessor, newFlags);
            }
         }
      }
   }
}
