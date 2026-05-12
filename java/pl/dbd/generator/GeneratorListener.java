package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

import java.util.*;

/**
 * GeneratorListener z AUTOMATYCZNĄ naprawą
 * 
 * System:
 * 1. Gracz klika PPM → zaczyna naprawę
 * 2. Progress rośnie automatycznie co tick
 * 3. Co 10-20s pojawia się skill check
 * 4. Gracz musi trafić w skill check
 * 5. Naprawa trwa aż do 100%
 */
public class GeneratorListener implements Listener {

    private final DBDPlugin plugin;
    private final Map<UUID, RepairData> repairingPlayers = new HashMap<>();
    private final Map<UUID, BukkitRunnable> carryTasks = new HashMap<>();
    private final Map<UUID, BukkitRunnable> hookTasks = new HashMap<>();

    private static final int REPAIR_TICKS = 1800; // 90 sekund pełna naprawa
    private static final int MIN_SKILL_CHECKS = 5; // Minimum 5 skill checków

    private static class RepairData {
        Generator generator;
        int ticksRepairing;
        int skillChecksDone;
        List<Integer> scheduledSkillChecks;

        RepairData(Generator generator) {
            this.generator = generator;
            this.ticksRepairing = 0;
            this.skillChecksDone = 0;
            this.scheduledSkillChecks = generateSchedule();
        }

        private List<Integer> generateSchedule() {
            List<Integer> schedule = new ArrayList<>();
            int count = MIN_SKILL_CHECKS + new Random().nextInt(3);
            int interval = REPAIR_TICKS / (count + 1);
            for (int i = 1; i <= count; i++) {
                int tick = interval * i + new Random().nextInt(-100, 100);
                tick = Math.max(200, Math.min(REPAIR_TICKS - 200, tick));
                schedule.add(tick);
            }
            Collections.sort(schedule);
            return schedule;
        }
    }

    public GeneratorListener(DBDPlugin plugin) {
        this.plugin = plugin;
        startRepairTask();
    }

    private void startRepairTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, RepairData> entry : new HashMap<>(repairingPlayers).entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    RepairData data = entry.getValue();

                    if (player == null || !player.isOnline()) {
                        repairingPlayers.remove(entry.getKey());
                        continue;
                    }

                    if (!player.getWorld().equals(data.generator.getLocation().getWorld())
                            || player.getLocation().distance(data.generator.getLocation()) > 3.0) {
                        player.sendMessage(pl.dbd.DBDPlugin.getMsg("coddalonosiodgenerat"));
                        stopRepairing(player);
                        continue;
                    }

                    if (data.generator.isCompleted()) {
                        stopRepairing(player);
                        continue;
                    }

                    data.ticksRepairing++;

                    // Dodaj progress automatycznie
                    double progressDelta = 100.0 / REPAIR_TICKS;
                    data.generator.setProgress(data.generator.getProgress() + progressDelta);

                    // SKILL CHECK w zaplanowanym momencie
                    if (!data.scheduledSkillChecks.isEmpty() &&
                            data.ticksRepairing >= data.scheduledSkillChecks.get(0)) {
                        data.scheduledSkillChecks.remove(0);
                        openSkillCheck(player, data);
                    }

                    // Efekty co sekundę
                    if (data.ticksRepairing % 20 == 0) {
                        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_EXTEND, 0.3f, 1.5f);
                        data.generator.getLocation().getWorld().spawnParticle(
                                Particle.SMOKE,
                                data.generator.getLocation().clone().add(0.5, 1, 0.5),
                                3, 0.2, 0.2, 0.2, 0.01);
                    }

                    // Action bar
                    player.sendActionBar(buildProgressBar(data.generator.getProgress()));

                    // Ukończony?
                    if (data.generator.getProgress() >= 100) {
                        data.generator.setCompleted(true);
                        onGeneratorCompleted(player, data.generator);
                        stopRepairing(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND)
            return;
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK)
            return;
        if (e.getClickedBlock() == null)
            return;

        Generator gen = plugin.getGeneratorManager().getGeneratorAt(e.getClickedBlock().getLocation());

        if (gen != null) {
            e.setCancelled(true);
            Player p = e.getPlayer();

            if (action == Action.LEFT_CLICK_BLOCK) {
                if (plugin.getGameManager().isKiller(p)) {
                    if (gen.isCompleted()) {
                        p.sendMessage(pl.dbd.DBDPlugin.getMsg("atengeneratorjestjun"));
                        return;
                    }
                    if (gen.getProgress() <= 0) {
                        p.sendMessage("§cTen generator nie ma żadnego progresu!");
                        return;
                    }
                    if (gen.isDamagedByKiller()) {
                        p.sendMessage(
                                "§cTen generator został już uszkodzony! Ocalały musi wykonać na nim skill-check.");
                        return;
                    }

                    gen.setProgress(Math.max(0, gen.getProgress() - 5.0));
                    gen.setDamagedByKiller(true);
                    p.sendMessage("§cUszkodziłeś generator (-5%)!");

                    gen.getLocation().getWorld().spawnParticle(
                            Particle.DUST,
                            gen.getLocation().clone().add(0.5, 1.2, 0.5),
                            30, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(org.bukkit.Color.BLACK, 2.0f));
                    gen.getLocation().getWorld().playSound(gen.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f,
                            0.5f);
                }
                return;
            }

            // Odtąd tylko RIGHT_CLICK_BLOCK dla ocalałych
            if (plugin.getGameManager().isKiller(p)) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cjakokillermoeszuszk"));
                return;
            }

            if (!plugin.getGameManager().isSurvivor(p) && !p.hasPermission("dbd.admin")) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("ctylkoocalalimognapr"));
                return;
            }

            if (gen.isCompleted()) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("atengeneratorjestjun"));
                return;
            }

            if (repairingPlayers.containsKey(p.getUniqueId())) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cjunaprawiaszgenerat"));
                return;
            }

            startRepairing(p, gen);
        }
    }

    private void startRepairing(Player player, Generator generator) {
        player.sendMessage(pl.dbd.DBDPlugin.getMsg("arozpoczynamnaprawge"));
        repairingPlayers.put(player.getUniqueId(), new RepairData(generator));
    }

    private void stopRepairing(Player player) {
        repairingPlayers.remove(player.getUniqueId());
    }

    private void openSkillCheck(Player player, RepairData data) {
        // Otwórz SkillCheckGUI
        new SkillCheckGUI(plugin, player, data.generator).open();
    }

    private void onGeneratorCompleted(Player p, Generator gen) {
        String countStr = plugin.getConfig().getString("generator-rewards.points-pool", "150");
        int count = Integer.parseInt(countStr);
        gen.triggerRedAlert();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p != null && p.isOnline()) {
                plugin.getSoulsManager().add(p, count);
                p.sendMessage("§aZdobądź: §e" + count + " §aDusz za ukończenie naprawy!");
            }
            gen.getLocation().getWorld().spawnParticle(
                    Particle.SCULK_SOUL,
                    gen.getLocation().clone().add(0.5, 1.5, 0.5),
                    30, 0.5, 0.5, 0.5, 0.05);
        }, 20L);

        Bukkit.broadcastMessage("§a§l✓ Generator ukończony! §7(" +
                plugin.getGeneratorManager().getCompletedCount() + "/" + plugin.getGeneratorManager().getTotalCount()
                + ")");

        if (plugin.getGeneratorManager().getCompletedCount() >= plugin.getGameManager().getRequiredGenerators()) {
            plugin.getExitGateManager().triggerGatesAura();
        }

        gen.getLocation().getWorld().playSound(
                gen.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        gen.getLocation().getWorld().spawnParticle(
                Particle.FIREWORK,
                gen.getLocation().clone().add(0.5, 1.5, 0.5),
                50, 0.5, 0.5, 0.5, 0.15);

        p.sendTitle("§a§lGENERATOR UKOŃCZONY!", "", 10, 40, 10);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null)
            return;
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SkillCheckGUI) {
            event.setCancelled(true);
            if (event.getClickedInventory().equals(event.getInventory())) {
                SkillCheckGUI gui = (SkillCheckGUI) holder;
                gui.handleClick(event.getSlot());
            }
        }
    }

    public void startCarrySkillChecks(Player survivor, Player killer) {
        stopCarrySkillChecks(survivor);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!survivor.isOnline() || !plugin.getCarrySystem().isCarrying(killer)) {
                    this.cancel();
                    carryTasks.remove(survivor.getUniqueId());
                    return;
                }
                if (survivor.getOpenInventory().getTopInventory().getHolder() instanceof SkillCheckGUI) {
                    return;
                }
                new SkillCheckGUI(plugin, survivor, killer).open();
            }
        };
        long interval = 100L + new Random().nextInt(60);
        task.runTaskTimer(plugin, interval, interval);
        carryTasks.put(survivor.getUniqueId(), task);
    }

    public void stopCarrySkillChecks(Player p) {
        BukkitRunnable t = carryTasks.remove(p.getUniqueId());
        if (t != null)
            t.cancel();
    }

    public void startHookStruggleChecks(Player survivor) {
        stopHookStruggleChecks(survivor);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!survivor.isOnline() || !plugin.getHookManager().isPlayerHooked(survivor.getUniqueId())) {
                    this.cancel();
                    hookTasks.remove(survivor.getUniqueId());
                    return;
                }
                if (survivor.getOpenInventory().getTopInventory().getHolder() instanceof SkillCheckGUI) {
                    return;
                }
                new SkillCheckGUI(plugin, survivor, SkillCheckGUI.SkillCheckType.HOOK_STRUGGLE).open();
            }
        };
        long interval = 160L + new Random().nextInt(80);
        task.runTaskTimer(plugin, interval, interval);
        hookTasks.put(survivor.getUniqueId(), task);
    }

    public void stopHookStruggleChecks(Player p) {
        BukkitRunnable t = hookTasks.remove(p.getUniqueId());
        if (t != null)
            t.cancel();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE)
            return;
        if (plugin.getGeneratorManager().getGeneratorAt(e.getBlock().getLocation()) != null) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(pl.dbd.DBDPlugin.getMsg("cniemoeszniszczygene"));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopRepairing(player);
        stopCarrySkillChecks(player);
        stopHookStruggleChecks(player);
    }

    public void cleanupAll() {
        for (BukkitRunnable t : carryTasks.values()) {
            if (t != null && !t.isCancelled())
                t.cancel();
        }
        for (BukkitRunnable t : hookTasks.values()) {
            if (t != null && !t.isCancelled())
                t.cancel();
        }
        carryTasks.clear();
        hookTasks.clear();
        repairingPlayers.clear();
    }

    private String buildProgressBar(double percent) {
        int total = 20;
        int filled = (int) Math.round((percent / 100.0) * total);
        filled = Math.max(0, Math.min(total, filled));
        String fillColor = percent >= 75 ? "§a" : percent >= 50 ? "§e" : percent >= 25 ? "§6" : "§c";
        StringBuilder bar = new StringBuilder();
        bar.append("§7[").append(fillColor);
        for (int i = 0; i < filled; i++)
            bar.append("█");
        bar.append("§8");
        for (int i = filled; i < total; i++)
            bar.append("█");
        bar.append("§7] §f").append(String.format("%.0f", percent)).append("%");
        return bar.toString();
    }
}