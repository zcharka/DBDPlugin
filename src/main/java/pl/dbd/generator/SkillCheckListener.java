package pl.dbd.generator;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

public class SkillCheckListener implements Listener {

    private final DBDPlugin plugin;

    // Taski dla automatycznych skill checków
    private final Map<UUID, BukkitRunnable> carryTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> hookTasks = new HashMap<>();

    public SkillCheckListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;

        InventoryHolder holder = event.getInventory().getHolder();

        // Sprawdzamy, czy to nasze SkillCheckGUI
        if (holder instanceof SkillCheckGUI) {
            event.setCancelled(true); // Blokujemy wyjmowanie szkła

            if (event.getClickedInventory().equals(event.getInventory())) {
                SkillCheckGUI gui = (SkillCheckGUI) holder;
                gui.handleClick(event.getSlot());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof SkillCheckGUI) {
            SkillCheckGUI gui = (SkillCheckGUI) holder;
            gui.forceFail(false);
        }
    }

    // ═══════════════════════════════════════════════
    // CARRY SKILL CHECKS - 2% ucieczka przy trafieniu
    // ═══════════════════════════════════════════════

    public void startCarrySkillChecks(Player survivor, Player killer) {
        stopCarrySkillChecks(survivor); // Cleanup poprzedniego

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!survivor.isOnline() || !plugin.getCarrySystem().isCarrying(killer)) {
                    this.cancel();
                    carryTasks.remove(survivor.getUniqueId());
                    return;
                }

                // Otwórz carry skill check
                SkillCheckGUI gui = new SkillCheckGUI(plugin, survivor, killer);
                gui.open();
            }
        };

        // Co 5-8 sekund
        long interval = 100L + new Random().nextInt(60);
        task.runTaskTimer(plugin, interval, interval);
        carryTasks.put(survivor.getUniqueId(), task);
    }

    public void stopCarrySkillChecks(Player survivor) {
        BukkitRunnable task = carryTasks.remove(survivor.getUniqueId());
        if (task != null)
            task.cancel();
    }

    // ═══════════════════════════════════════════════
    // HOOK STAGE 2 STRUGGLE - miss = śmierć
    // ═══════════════════════════════════════════════

    public void startHookStruggleChecks(Player survivor) {
        stopHookStruggleChecks(survivor); // Cleanup poprzedniego

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!survivor.isOnline() || !plugin.getHookManager().isPlayerHooked(survivor.getUniqueId())) {
                    this.cancel();
                    hookTasks.remove(survivor.getUniqueId());
                    return;
                }

                // Otwórz hook struggle skill check
                SkillCheckGUI gui = new SkillCheckGUI(plugin, survivor, SkillCheckGUI.SkillCheckType.HOOK_STRUGGLE);
                gui.open();
            }
        };

        // Co 8-12 sekund
        long interval = 160L + new Random().nextInt(80);
        task.runTaskTimer(plugin, interval, interval);
        hookTasks.put(survivor.getUniqueId(), task);
    }

    public void stopHookStruggleChecks(Player survivor) {
        BukkitRunnable task = hookTasks.remove(survivor.getUniqueId());
        if (task != null)
            task.cancel();
    }

    // ═══════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopCarrySkillChecks(player);
        stopHookStruggleChecks(player);
    }

    public void cleanupAll() {
        for (BukkitRunnable task : new ArrayList<>(carryTasks.values())) {
            task.cancel();
        }
        for (BukkitRunnable task : new ArrayList<>(hookTasks.values())) {
            task.cancel();
        }
        carryTasks.clear();
        hookTasks.clear();
    }
}