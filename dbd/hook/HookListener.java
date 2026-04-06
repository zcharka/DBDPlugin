package pl.dbd.hook;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;
import pl.dbd.DBDPlugin;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class HookListener implements Listener {
    private final DBDPlugin plugin;
    private final Map<UUID, UnhookTask> unhookingTasks = new HashMap<>();

    public HookListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND)
            return;

        Player p = e.getPlayer();
        Action a = e.getAction();

        if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            if (unhookingTasks.containsKey(p.getUniqueId())) {
                unhookingTasks.get(p.getUniqueId()).cancelUnhook();
            }
        }

        // Znajdź najbliższy hak zamiast wymagać precyzyjnego celowania w blok
        Hook h = null;
        for (Hook hook : plugin.getHookManager().getAllHooks()) {
            if (hook.getLocation().getWorld() != null && hook.getLocation().getWorld().equals(p.getWorld())) {
                if (hook.getLocation().distance(p.getLocation()) < 3.5) {
                    h = hook;
                    break;
                }
            }
        }

        if (h != null) {
            if (h.isOccupied()) {
                if (a == Action.RIGHT_CLICK_BLOCK || a == Action.RIGHT_CLICK_AIR) {
                    if (plugin.getGameManager().isKiller(p)) {
                        p.sendMessage("§cKiller nie może zdejmować ocalałych z haka!");
                        return;
                    }
                    if (p.getUniqueId().equals(h.getHookedPlayer().getUniqueId())) {
                        p.sendMessage("§cNie możesz sam siebie zdjąć w ten sposób!");
                        return;
                    }

                    pl.dbd.state.PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
                    if (state == pl.dbd.state.PlayerStateManager.PlayerState.HOOKED ||
                            state == pl.dbd.state.PlayerStateManager.PlayerState.DOWNED ||
                            state == pl.dbd.state.PlayerStateManager.PlayerState.CARRIED ||
                            state == pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
                        p.sendMessage("§cNie możesz ratować innych w swoim aktualnym stanie!");
                        return;
                    }

                    // Rozpoczęcie zdejmowania z haka
                    if (!unhookingTasks.containsKey(p.getUniqueId())) {
                        UnhookTask task = new UnhookTask(p, h.getHookedPlayer(), h);
                        unhookingTasks.put(p.getUniqueId(), task);
                        task.runTaskTimer(plugin, 0L, 1L);
                    }
                }
            } else {
                // Próba powieszenia gracza przez Killera
                if (plugin.getGameManager().isKiller(p) && plugin.getCarrySystem().isCarrying(p)) {
                    // Po lewym lub prawym kliknięciu w stronę haka, wieszamy i anulujemy by nie
                    // zniszczył
                    e.setCancelled(true);
                    Player survivor = plugin.getCarrySystem().getCarriedSurvivor(p);
                    if (survivor != null) {
                        plugin.getCarrySystem().stopCarrying(p);
                        plugin.getHookManager().hookPlayer(survivor, h);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();

        if (unhookingTasks.containsKey(p.getUniqueId())) {
            unhookingTasks.get(p.getUniqueId()).cancelUnhook();
        }

        if (e.isSneaking() && plugin.getStateManager().isHooked(p)) {
            Hook h = plugin.getHookManager().getHookByPlayer(p);
            if (h != null && h.canSelfUnhook()) {
                plugin.getHookManager().unhookPlayer(p);
                p.sendMessage("§aUdało ci się uciec z haka (Anti-Facecamp)!");
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND)
            return;

        if (!(e.getRightClicked() instanceof Player))
            return;

        Player p = e.getPlayer();
        Player target = (Player) e.getRightClicked();

        if (plugin.getGameManager().isKiller(p)) {
            return;
        }

        if (plugin.getStateManager().isHooked(target)) {
            Hook h = plugin.getHookManager().getHookByPlayer(target);
            if (h != null) {
                pl.dbd.state.PlayerStateManager.PlayerState state = plugin.getStateManager().getState(p);
                if (state == pl.dbd.state.PlayerStateManager.PlayerState.HOOKED ||
                        state == pl.dbd.state.PlayerStateManager.PlayerState.DOWNED ||
                        state == pl.dbd.state.PlayerStateManager.PlayerState.CARRIED ||
                        state == pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
                    p.sendMessage("§cNie możesz ratować innych w swoim aktualnym stanie!");
                    return;
                }

                if (!unhookingTasks.containsKey(p.getUniqueId())) {
                    UnhookTask task = new UnhookTask(p, target, h);
                    unhookingTasks.put(p.getUniqueId(), task);
                    task.runTaskTimer(plugin, 0L, 1L);
                }
            }
        }
    }

    // --- METODY WYMAGANE PRZEZ GameManager i SkillCheckGUI ---

    public void cleanupAllHooks() {
        plugin.getHookManager().resetAllHooks();
    }

    public void resetPlayerHookCount(UUID uuid) {
        plugin.getHookManager().resetPlayerHookCount(uuid);
    }

    public void forceStage3Death(Player p) {
        if (p != null) {
            Hook h = plugin.getHookManager().getHookByPlayer(p);
            if (h != null) {
                h.getLocation().getWorld().strikeLightningEffect(h.getLocation());
            }

            plugin.getGameManager().markHookDead(p);
            plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.DEAD);
            plugin.getHookManager().unhookPlayer(p);

            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setWalkSpeed(0.2F);
            for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            p.setGlowing(false);
            if (plugin.getGameManager() != null) {
                plugin.getGameManager().setPlayerRedGlow(p, false);
            }
            p.getInventory().clear();

            if (plugin.getGameManager().getHookedSpawn() != null) {
                p.teleport(plugin.getGameManager().getHookedSpawn());
            } else if (plugin.getGameManager().getKilledSpawn() != null) {
                p.teleport(plugin.getGameManager().getKilledSpawn());
            } else if (plugin.getGameManager().getLobbySpawn() != null) {
                p.teleport(plugin.getGameManager().getLobbySpawn());
            }

            // Tryb widza - gracz może latać i obserwować
            p.setGameMode(org.bukkit.GameMode.SPECTATOR);

            p.sendMessage("§cZostałeś poświęcony...");
            p.sendTitle("§c§lUMARŁ", "§7Możesz teraz obserwować mecz", 10, 60, 20);

            // Nie resetujemy parenta! Tylko dodajemy permisję oglądania z priorytetem 100
            try {
                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                            "lp user " + p.getName() + " permission set dbd.oglada true");
                }
            } catch (Exception ex) {
            }
        }
    }
    // --- KLASA ZADANIA: ZDEJMOWANIE ---

    private class UnhookTask extends BukkitRunnable {
        private final Player rescuer;
        private final Player target;
        private final Hook hook;
        private final org.bukkit.boss.BossBar bossBar;
        private int ticks = 0;
        private final Location startLoc;

        public UnhookTask(Player rescuer, Player target, Hook hook) {
            this.rescuer = rescuer;
            this.target = target;
            this.hook = hook;
            this.startLoc = rescuer.getLocation().clone();
            this.bossBar = org.bukkit.Bukkit.createBossBar("§cZDEJMOWANIE...", org.bukkit.boss.BarColor.RED,
                    org.bukkit.boss.BarStyle.SOLID);
            this.bossBar.addPlayer(rescuer);
            rescuer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 100,
                    5, false, false, false));
        }

        @Override
        public void run() {
            if (!rescuer.isOnline() || !target.isOnline() || !hook.isOccupied() || hook.getHookedPlayer() != target) {
                cancelUnhook();
                return;
            }

            // Przerwanie podczas przemieszczenia siê / rozgl¹dania
            Location current = rescuer.getLocation();
            if (current.distanceSquared(startLoc) > 0.1) {
                cancelUnhook();
                return;
            }
            if (Math.abs(current.getYaw() - startLoc.getYaw()) > 45
                    || Math.abs(current.getPitch() - startLoc.getPitch()) > 45) {
                cancelUnhook();
                return;
            }

            ticks++;
            double progress = ticks / 60.0;
            bossBar.setProgress(Math.min(1.0, progress));

            if (ticks >= 60) {
                this.cancel();
                unhookingTasks.remove(rescuer.getUniqueId());
                bossBar.removeAll();
                rescuer.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
                plugin.getHookManager().unhookPlayer(target, rescuer);
            }
        }

        public void cancelUnhook() {
            this.cancel();
            unhookingTasks.remove(rescuer.getUniqueId());
            bossBar.removeAll();
            rescuer.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
            rescuer.sendMessage("§ePrzerwano zdejmowanie z haka.");
        }
    }
}