package pl.dbd.generator;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

public class SkillCheckGUI implements InventoryHolder {
   private final DBDPlugin plugin;
   private final Player player;
   private final Generator generator;
   private final Inventory inventory;
   private final SkillCheckType type;
   private Player killer; // tylko dla CARRY
   
   // STATYCZNE STREFY (nie zmieniają pozycji)
   private int greatZoneSlot; // 1 żółte pole (great)
   private int successZoneStart; // początek 2 zielonych pól
   private int successZoneEnd; // koniec 2 zielonych pól
   
   // PRZESUWAJĄCY SIĘ WSKAŹNIK
   private int pointerPosition = 9; // Startuje od lewej (slot 9)
   
   private boolean hasClicked = false;
   private boolean isActive = true;
   private BukkitRunnable pointerTask;

   public enum SkillCheckType {
      GENERATOR,
      CARRY,
      HOOK_STRUGGLE
   }

   // === KONSTRUKTOR DLA GENERATORA ===
   public SkillCheckGUI(DBDPlugin plugin, Player player, Generator generator) {
      this.plugin = plugin;
      this.player = player;
      this.generator = generator;
      this.type = SkillCheckType.GENERATOR;
      this.killer = null;
      this.inventory = Bukkit.createInventory(this, 54, "§8Skill Check (Refleks)");
      
      this.setupStaticGUI();
      this.startPointerMovement();
   }

   // === KONSTRUKTOR DLA CARRY ===
   public SkillCheckGUI(DBDPlugin plugin, Player survivor, Player killer) {
      this.plugin = plugin;
      this.player = survivor;
      this.generator = null;
      this.type = SkillCheckType.CARRY;
      this.killer = killer;
      this.inventory = Bukkit.createInventory(this, 54, "§4STRUGGLE!");
      
      this.setupStaticGUI();
      this.startPointerMovement();
   }

   // === KONSTRUKTOR DLA HOOK STRUGGLE ===
   public SkillCheckGUI(DBDPlugin plugin, Player survivor, SkillCheckType hookType) {
      this.plugin = plugin;
      this.player = survivor;
      this.generator = null;
      this.type = hookType;
      this.killer = null;
      this.inventory = Bukkit.createInventory(this, 54, "§4STRUGGLE - DON'T MISS!");
      
      this.setupStaticGUI();
      this.startPointerMovement();
   }

   private void setupStaticGUI() {
      // CZARNE TŁO na górze i dole
      ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
      ItemMeta blackMeta = blackGlass.getItemMeta();
      blackMeta.setDisplayName(" ");
      blackGlass.setItemMeta(blackMeta);

      // CZERWONE TŁO (miss)
      ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
      ItemMeta redMeta = redGlass.getItemMeta();
      redMeta.setDisplayName("§c§lMISS!");
      redGlass.setItemMeta(redMeta);

      // Wypełniamy wszystko
      for(int i = 0; i < 54; ++i) {
         if (i < 9 || i >= 45) {
            inventory.setItem(i, blackGlass); // Góra i dół
         } else {
            inventory.setItem(i, redGlass); // Środek (skill check area)
         }
      }

      // LOSUJEMY POZYCJĘ STREF (w środkowym obszarze 9-44)
      // Obszar: 36 slotów, podzielony na 4 rzędy po 9 slotów
      
      // Losujemy początek strefy sukcesu (z marginesem)
      this.successZoneStart = 12 + new Random().nextInt(20); // Sloty 12-31
      this.successZoneEnd = this.successZoneStart + 1; // 2 pola zielone
      
      // Great zone PRZED zieloną strefą (1 pole żółte)
      this.greatZoneSlot = this.successZoneStart - 1;
      
      // Ustawiamy STATYCZNE strefy
      
      // ŻÓŁTA STREFA (Great - 1 pole)
      ItemStack yellowGlass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
      ItemMeta yellowMeta = yellowGlass.getItemMeta();
      yellowMeta.setDisplayName("§6§l★ GREAT! (+2% bonus)");
      yellowGlass.setItemMeta(yellowMeta);
      inventory.setItem(greatZoneSlot, yellowGlass);
      
      // ZIELONE STREFY (Good - 2 pola)
      ItemStack greenGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
      ItemMeta greenMeta = greenGlass.getItemMeta();
      
      String goodText = switch(type) {
         case GENERATOR -> "§a§lGOOD";
         case CARRY -> "§a§lGOOD (2% escape)";
         case HOOK_STRUGGLE -> "§a§lSURVIVE";
      };
      greenMeta.setDisplayName(goodText);
      greenGlass.setItemMeta(greenMeta);
      
      inventory.setItem(successZoneStart, greenGlass);
      inventory.setItem(successZoneEnd, greenGlass);
   }

   private void startPointerMovement() {
      pointerTask = new BukkitRunnable() {
         public void run() {
            if (!isActive || !player.isOnline() || 
                !player.getOpenInventory().getTopInventory().equals(inventory)) {
               this.cancel();
               isActive = false;
               return;
            }

            // CZYŚCIMY poprzednią pozycję wskaźnika (przywracamy tło)
            if (pointerPosition >= 9 && pointerPosition < 45) {
               // Sprawdzamy co tam było
               if (pointerPosition == greatZoneSlot) {
                  // Przywróć żółte
                  ItemStack yellowGlass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
                  ItemMeta yellowMeta = yellowGlass.getItemMeta();
                  yellowMeta.setDisplayName("§6§l★ GREAT! (+2% bonus)");
                  yellowGlass.setItemMeta(yellowMeta);
                  inventory.setItem(pointerPosition, yellowGlass);
               } else if (pointerPosition >= successZoneStart && pointerPosition <= successZoneEnd) {
                  // Przywróć zielone
                  ItemStack greenGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                  ItemMeta greenMeta = greenGlass.getItemMeta();
                  String goodText = switch(type) {
                     case GENERATOR -> "§a§lGOOD";
                     case CARRY -> "§a§lGOOD (2% escape)";
                     case HOOK_STRUGGLE -> "§a§lSURVIVE";
                  };
                  greenMeta.setDisplayName(goodText);
                  greenGlass.setItemMeta(greenMeta);
                  inventory.setItem(pointerPosition, greenGlass);
               } else {
                  // Przywróć czerwone
                  ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                  ItemMeta redMeta = redGlass.getItemMeta();
                  redMeta.setDisplayName("§c§lMISS!");
                  redGlass.setItemMeta(redMeta);
                  inventory.setItem(pointerPosition, redGlass);
               }
            }

            // PRZESUWAMY WSKAŹNIK
            pointerPosition++;
            
            // Pomijamy czarne rzędy (0-8 i 45-53)
            if (pointerPosition >= 45) {
               pointerPosition = 9; // Reset do początku
            }
            
            // RYSUJEMY WSKAŹNIK na nowej pozycji
            ItemStack pointer = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            ItemMeta pointerMeta = pointer.getItemMeta();
            pointerMeta.setDisplayName("§f§l▼ KLIKNIJ TERAZ! ▼");
            pointer.setItemMeta(pointerMeta);
            inventory.setItem(pointerPosition, pointer);
            
            // Dźwięk tykania
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2F, 2.0F);
         }
      };
      
      // Szybkość przesuwania (co 3 ticki = 0.15s)
      long speed = switch(type) {
         case GENERATOR -> 3L;
         case CARRY -> 2L; // Szybciej (trudniejsze)
         case HOOK_STRUGGLE -> 3L;
      };
      
      pointerTask.runTaskTimer(plugin, 0L, speed);
   }

   public void handleClick(int clickedSlot) {
      if (!isActive || hasClicked) return;
      
      hasClicked = true;
      isActive = false;
      if (pointerTask != null) pointerTask.cancel();

      // Sprawdzamy gdzie był wskaźnik
      boolean hitGreat = (pointerPosition == greatZoneSlot);
      boolean hitGood = (pointerPosition >= successZoneStart && pointerPosition <= successZoneEnd);
      
      if (hitGreat) {
         successGreat();
      } else if (hitGood) {
         successGood();
      } else {
         fail();
      }

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
         player.closeInventory();
      }, 5L);
   }

   private void successGreat() {
      switch(type) {
         case GENERATOR -> {
            player.sendMessage("§6§l★ GREAT SKILL CHECK! §e(+2% Bonus)");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
            generator.addProgress(2.0D);
         }
         case CARRY -> {
            // 2% SZANSA NA UCIECZKĘ
            double chance = Math.random();
            if (chance < 0.02) {
               player.sendMessage("§a§l✓ UCIEKŁEŚ Z NOSZENIA!");
               if (killer != null) {
                  killer.sendMessage("§c§l✗ " + player.getName() + " uciekł!");
                  killer.playSound(killer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 0.8F);
               }
               player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 2.0F);
               plugin.getCarrySystem().stopCarrying(killer);
            } else {
               player.sendMessage("§6§lGREAT! §7Ale nie udało się uciec... (2%)");
               player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
            }
         }
         case HOOK_STRUGGLE -> {
            player.sendMessage("§6§l★ GREAT! §7Przeżyłeś!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
         }
      }
   }

   private void successGood() {
      switch(type) {
         case GENERATOR -> {
            player.sendMessage("§a§l✓ GOOD SKILL CHECK!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            // Brak bonusu, ale też bez kary
         }
         case CARRY -> {
            player.sendMessage("§a§lGOOD!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0F, 1.0F);
         }
         case HOOK_STRUGGLE -> {
            player.sendMessage("§a§l✓ GOOD - Przeżyłeś!");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
         }
      }
   }

   private void fail() {
      switch(type) {
         case GENERATOR -> {
            player.sendMessage("§c§l✗ MISS! §7(-5% Progress)");
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 2.0F);
            generator.failSkillCheck();
            generator.notifyGeneratorInterrupted();
            notifyKiller();
         }
         case CARRY -> {
            player.sendMessage("§c§lMISS!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5F, 0.8F);
         }
         case HOOK_STRUGGLE -> {
            player.sendMessage("§4§l✗ ZGINĄŁEŚ! §7(Failed Struggle)");
            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5F, 0.5F);
            Bukkit.broadcastMessage("§c☠ " + player.getName() + " §7nie zdołał przetrwać struggle!");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
               plugin.getHookListener().forceStage3Death(player);
            }, 10L);
         }
      }
   }
   
   private void notifyKiller() {
      for (Player p : Bukkit.getOnlinePlayers()) {
         if (p.hasPermission("dbd.killer")) {
             p.sendMessage("§c§l! GENERATOR EKSPLODOWAŁ !");
             p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5F, 0.5F);
             p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 0, false, false));
         }
      }
   }

   public void open() {
      player.openInventory(inventory);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.5F);
   }

   @Override
   public Inventory getInventory() {
      return inventory;
   }
}