package pl.dbd.carry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class CarrySystem implements Listener {

    private final DBDPlugin plugin;
    private final Map<UUID, UUID> carrying = new HashMap<>(); 
    private final Map<UUID, BukkitRunnable> dragTasks = new HashMap<>();
    
    private final Map<UUID, Double> wiggleProgress = new HashMap<>();
    private final Map<UUID, WiggleCheckData> activeWiggleChecks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> wiggleSpawners = new HashMap<>();

    private static final int WIGGLE_SC_SIZE = 27;

    private static class WiggleCheckData {
        int successStart, successEnd, pointerSlot;
        WiggleCheckData() {
            int zoneStart = ThreadLocalRandom.current().nextInt(0, WIGGLE_SC_SIZE - 4);
            this.successStart = zoneStart;
            this.successEnd = zoneStart + 3; 
            this.pointerSlot = ThreadLocalRandom.current().nextInt(0, WIGGLE_SC_SIZE);
        }
        boolean isInZone(int slot) { return slot >= successStart && slot <= successEnd; }
    }

    // POPRAWIONY KONSTRUKTOR (przyjmuje tylko Plugin)
    public CarrySystem(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    public void pickUp(Player killer, Player survivor) {
        if (carrying.containsValue(survivor.getUniqueId())) return;
        startCarrying(killer, survivor);
        killer.sendMessage("§ePodniosłeś gracza: " + survivor.getName());
        survivor.sendMessage("§cJesteś niesiony przez killera! §eWykonuj Testy Umiejętności, by się wyrwać!");
    }

    // TEJ METODY BRAKOWAŁO (naprawia błąd w CarryListener_FIXED)
    public void dropSurvivor(Player killer) {
        if (isCarrying(killer)) {
            stopCarrying(killer);
            killer.sendMessage("§cUpuściłeś ocalałego.");
        }
    }

    private void startCarrying(Player killer, Player survivor) {
        carrying.put(killer.getUniqueId(), survivor.getUniqueId());

        survivor.removePotionEffect(PotionEffectType.SLOWNESS);
        survivor.removePotionEffect(PotionEffectType.JUMP_BOOST);
        survivor.setWalkSpeed(0.0f);
        survivor.setCollidable(false);
        killer.setCollidable(false);
        survivor.setGravity(false); 
        survivor.setGliding(false);

        plugin.getStateManager().setState(survivor, PlayerStateManager.PlayerState.CARRIED);
        startDragging(killer, survivor);
        startWiggleSystem(survivor);
    }

    private void startWiggleSystem(Player survivor) {
        wiggleProgress.put(survivor.getUniqueId(), 0.0);
        
        BukkitRunnable spawner = new BukkitRunnable() {
            @Override
            public void run() {
                if (!carrying.containsValue(survivor.getUniqueId()) || !survivor.isOnline()) {
                    this.cancel(); return;
                }
                if (survivor.getOpenInventory().getTitle().equals("§c§lWYRYWANIE (WIGGLE)")) return;
                openWiggleGUI(survivor);
            }
        };
        spawner.runTaskTimer(plugin, 40L, 60L);
        wiggleSpawners.put(survivor.getUniqueId(), spawner);
    }

    private void openWiggleGUI(Player survivor) {
        WiggleCheckData data = new WiggleCheckData();
        Inventory inv = Bukkit.createInventory(null, WIGGLE_SC_SIZE, "§c§lWYRYWANIE (WIGGLE)");
        
        ItemStack zone = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta zm = zone.getItemMeta(); zm.setDisplayName("§a§lSTREFA"); zone.setItemMeta(zm);
        
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta(); bm.setDisplayName(" "); bg.setItemMeta(bm);
        
        ItemStack ptr = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta pm = ptr.getItemMeta(); pm.setDisplayName("§f§lWSKAŹNIK"); ptr.setItemMeta(pm);

        activeWiggleChecks.put(survivor.getUniqueId(), data);
        survivor.openInventory(inv);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!activeWiggleChecks.containsKey(survivor.getUniqueId())) { this.cancel(); return; }
                WiggleCheckData cur = activeWiggleChecks.get(survivor.getUniqueId());
                cur.pointerSlot = (cur.pointerSlot + 3) % WIGGLE_SC_SIZE; 
                
                for (int i = 0; i < WIGGLE_SC_SIZE; i++) {
                    if (i == cur.pointerSlot) inv.setItem(i, ptr);
                    else if (cur.isInZone(i)) inv.setItem(i, zone);
                    else inv.setItem(i, bg);
                }
                if (cur.pointerSlot % 3 == 0) survivor.playSound(survivor.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.2f, 1.2f);
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeWiggleChecks.containsKey(survivor.getUniqueId())) {
                survivor.closeInventory();
                activeWiggleChecks.remove(survivor.getUniqueId());
            }
        }, 50L);
    }

    @EventHandler
    public void onWiggleClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§c§lWYRYWANIE (WIGGLE)")) return;
        event.setCancelled(true);
        Player survivor = (Player) event.getWhoClicked();
        
        WiggleCheckData data = activeWiggleChecks.remove(survivor.getUniqueId());
        if (data == null) return;
        
        int ptrPos = data.pointerSlot;
        survivor.closeInventory();
        
        if (data.isInZone(ptrPos)) {
            double prog = wiggleProgress.getOrDefault(survivor.getUniqueId(), 0.0) + 20.0;
            survivor.playSound(survivor.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            if (prog >= 100.0) escapeCarry(survivor);
            else {
                wiggleProgress.put(survivor.getUniqueId(), prog);
                survivor.sendTitle("", "§aWyrwanie: " + (int)prog + "%", 0, 20, 0);
            }
        } else {
            survivor.playSound(survivor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            survivor.sendTitle("", "§cPudło!", 0, 20, 0);
        }
    }
    
    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals("§c§lWYRYWANIE (WIGGLE)")) {
            activeWiggleChecks.remove(e.getPlayer().getUniqueId());
        }
    }

    private void escapeCarry(Player survivor) {
        Player killer = getKillerCarrying(survivor);
        if (killer != null) {
            stopCarrying(killer);
            plugin.getStateManager().setState(survivor, PlayerStateManager.PlayerState.INJURED);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, false));
            killer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, false, false));
            survivor.sendMessage("§a§lWYRWAŁEŚ SIĘ!");
        }
    }

    private Player getKillerCarrying(Player survivor) {
        for (Map.Entry<UUID, UUID> entry : carrying.entrySet()) {
            if (entry.getValue().equals(survivor.getUniqueId())) return Bukkit.getPlayer(entry.getKey());
        }
        return null;
    }

    private void startDragging(Player killer, Player survivor) {
        BukkitRunnable old = dragTasks.remove(killer.getUniqueId());
        if (old != null) old.cancel();

        BukkitRunnable drag = new BukkitRunnable() {
            @Override
            public void run() {
                if (!carrying.containsKey(killer.getUniqueId()) || !killer.isOnline() || !survivor.isOnline()) {
                    stopCarrying(killer); this.cancel(); return;
                }
                Location kLoc = killer.getLocation();
                Location target = kLoc.clone().add(kLoc.getDirection().multiply(0.5)).add(0, 1.2, 0);
                if (kLoc.distance(survivor.getLocation()) > 5) survivor.teleport(target);
                else {
                    Vector dir = target.toVector().subtract(survivor.getLocation().toVector());
                    survivor.setVelocity(dir.multiply(0.5));
                }
            }
        };
        drag.runTaskTimer(plugin, 0L, 1L);
        dragTasks.put(killer.getUniqueId(), drag);
    }

    public void stopCarrying(Player killer) {
        UUID survivorUUID = carrying.remove(killer.getUniqueId());
        BukkitRunnable drag = dragTasks.remove(killer.getUniqueId());
        if (drag != null) drag.cancel();
        killer.setCollidable(true);

        if (survivorUUID != null) {
            Player survivor = Bukkit.getPlayer(survivorUUID);
            if (survivor != null) {
                survivor.setGravity(true);
                survivor.setCollidable(true);
                wiggleProgress.remove(survivorUUID);
                if (wiggleSpawners.containsKey(survivorUUID)) wiggleSpawners.get(survivorUUID).cancel();
                activeWiggleChecks.remove(survivorUUID);
                
                PlayerStateManager.PlayerState current = plugin.getStateManager().getState(survivor);
                if (current == PlayerStateManager.PlayerState.CARRIED) {
                    plugin.getStateManager().setState(survivor, PlayerStateManager.PlayerState.DOWNED);
                    plugin.getStateManager().applyDownedState(survivor);
                }
            }
        }
    }

    public boolean isCarrying(Player killer) { return carrying.containsKey(killer.getUniqueId()); }
    
    public Player getCarriedSurvivor(Player killer) {
        UUID uuid = carrying.get(killer.getUniqueId());
        return uuid != null ? Bukkit.getPlayer(uuid) : null;
    }
}