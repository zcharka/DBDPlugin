package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KOMPLETNY GeneratorListener z integracją SkillCheckGUI
 * 
 * - Automatyczna naprawa generatora (progress rośnie co tick)
 * - Skill checki pojawiają się LOSOWO podczas naprawy
 * - SkillCheckGUI obsługuje GUI z przesuwającym się wskaźnikiem
 */
public class GeneratorListener implements Listener {
    
    private final DBDPlugin plugin;
    private final GeneratorManager generatorManager;
    private final Map<UUID, RepairData> repairingPlayers = new HashMap<>();
    
    private static final int REPAIR_TICKS = 1800; // 90 sekund na pełną naprawę
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
            this.scheduledSkillChecks = generateSkillCheckSchedule();
        }
        
        private List<Integer> generateSkillCheckSchedule() {
            List<Integer> schedule = new ArrayList<>();
            int skillChecks = MIN_SKILL_CHECKS + ThreadLocalRandom.current().nextInt(3); // 5-7 skill checków
            int interval = REPAIR_TICKS / (skillChecks + 1);
            
            for (int i = 1; i <= skillChecks; i++) {
                int tick = interval * i + ThreadLocalRandom.current().nextInt(-100, 100);
                tick = Math.max(200, Math.min(REPAIR_TICKS - 200, tick)); // Z marginesem
                schedule.add(tick);
            }
            Collections.sort(schedule);
            return schedule;
        }
    }
    
    public GeneratorListener(DBDPlugin plugin, GeneratorManager generatorManager) {
        this.plugin = plugin;
        this.generatorManager = generatorManager;
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
                    
                    // Sprawdź odległość
                    if (player.getLocation().distance(data.generator.getLocation()) > 3.0) {
                        player.sendMessage("§cOddalono się od generatora!");
                        stopRepairing(player);
                        continue;
                    }
                    
                    // Sprawdź czy ukończony
                    if (data.generator.isCompleted()) {
                        stopRepairing(player);
                        continue;
                    }
                    
                    data.ticksRepairing++;
                    
                    // Dodaj progress (100% / 1800 ticków = ~0.055% na tick)
                    double progressDelta = 100.0 / REPAIR_TICKS;
                    data.generator.addProgress(progressDelta);
                    
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
                            3, 0.2, 0.2, 0.2, 0.01
                        );
                    }
                    
                    // Action bar z progressem
                    player.sendActionBar(buildProgressBar(data.generator.getProgress()));
                    
                    // Ukończony?
                    if (data.generator.isCompleted()) {
                        onGeneratorCompleted(player, data.generator);
                        stopRepairing(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        event.setCancelled(true);
        Player player = event.getPlayer();
        Generator generator = generatorManager.getGeneratorAt(event.getClickedBlock().getLocation());
        
        if (generator == null) return;
        
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || gameManager.getGameState() != GameManager.GameState.IN_GAME) return;

        // Killer może niszczyć generatory (TODO)
        if (gameManager.isKiller(player)) {
            player.sendMessage("§cKiller może uszkodzić generator (TODO)");
            return;
        }
        
        if (!gameManager.isSurvivor(player)) return;
        if (generator.isCompleted() || repairingPlayers.containsKey(player.getUniqueId())) return;
        
        startRepairing(player, generator);
    }
    
    private void startRepairing(Player player, Generator generator) {
        player.sendMessage("§aRozpoczynam naprawę generatora...");
        repairingPlayers.put(player.getUniqueId(), new RepairData(generator));
    }
    
    private void stopRepairing(Player player) {
        repairingPlayers.remove(player.getUniqueId());
    }
    
    // ═══════════════════════════════════════════════════════════
    // INTEGRACJA Z SkillCheckGUI - NAJWAŻNIEJSZA METODA!
    // ═══════════════════════════════════════════════════════════
    private void openSkillCheck(Player player, RepairData repairData) {
        // SkillCheckGUI obsługuje WSZYSTKO:
        // - Przesuwający się wskaźnik
        // - Statyczne strefy (żółta + 2 zielone)
        // - Kliknięcia (GREAT/GOOD/MISS)
        // - Dodawanie/odbieranie progressu
        // - Efekty dźwiękowe i wizualne
        // - Powiadamianie killera
        
        SkillCheckGUI gui = new SkillCheckGUI(plugin, player, repairData.generator);
        gui.open();
    }
    
    private void onGeneratorCompleted(Player player, Generator generator) {
        Bukkit.broadcastMessage("§a§l✓ Generator ukończony! §7(" + 
            generatorManager.getCompletedCount() + "/" + generatorManager.getTotalCount() + ")");
        
        generator.getLocation().getWorld().playSound(
            generator.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        generator.getLocation().getWorld().spawnParticle(
            Particle.FIREWORK, 
            generator.getLocation().clone().add(0.5, 1.5, 0.5), 
            50, 0.5, 0.5, 0.5, 0.15);
        
        player.sendTitle("§a§lGENERATOR UKOŃCZONY!", "", 10, 40, 10);
        player.setGlowing(true);
        new BukkitRunnable() {
            @Override 
            public void run() { 
                if (player.isOnline()) player.setGlowing(false); 
            }
        }.runTaskLater(plugin, 60L);
    }
    
    @EventHandler 
    public void onPlayerQuit(PlayerQuitEvent event) { 
        stopRepairing(event.getPlayer()); 
    }
    
    public void cancelAllRepairs() {
        for (UUID uuid : new HashSet<>(repairingPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) stopRepairing(player);
        }
    }

    private String buildProgressBar(double percent) {
        int total = 20;
        int filled = (int) Math.round((percent / 100.0) * total);
        filled = Math.max(0, Math.min(total, filled));
        
        String fillColor = percent >= 75 ? "§a" : percent >= 50 ? "§e" : percent >= 25 ? "§6" : "§c";
        StringBuilder bar = new StringBuilder();
        
        bar.append("§7[").append(fillColor);
        for (int i = 0; i < filled; i++) bar.append("█");
        bar.append("§8");
        for (int i = filled; i < total; i++) bar.append("█");
        bar.append("§7] §f").append(String.format("%.0f", percent)).append("%");
        
        return bar.toString();
    }
}