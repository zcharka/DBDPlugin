package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pl.dbd.DBDPlugin;

import java.util.Random;

public class SkillCheckGUI implements InventoryHolder {
    private final DBDPlugin plugin;
    private final Player player;
    private final Generator generator;
    private final Inventory inventory;
    private final SkillCheckType type;

    private int currentPosition = 9;
    private int greatZoneSlot;
    private int successZoneStart;
    private int successZoneEnd;

    private boolean hasClicked = false;
    private boolean isActive = true;
    private BukkitRunnable rotationTask;
    private Player killer;

    public enum SkillCheckType {
        GENERATOR, CARRY, HOOK_STRUGGLE
    }

    public SkillCheckGUI(DBDPlugin plugin, Player player, Generator generator) {
        this.plugin = plugin;
        this.player = player;
        this.generator = generator;
        this.type = SkillCheckType.GENERATOR;
        this.killer = null;
        this.inventory = Bukkit.createInventory(this, 54, "§8Skill Check!");
        setupZones();
        startRotation();
    }

    public SkillCheckGUI(DBDPlugin plugin, Player survivor, Player killer) {
        this.plugin = plugin;
        this.player = survivor;
        this.generator = null;
        this.type = SkillCheckType.CARRY;
        this.killer = killer;
        this.inventory = Bukkit.createInventory(this, 54, "§4Wiggle!");
        setupZones();
        startRotation();
    }

    public SkillCheckGUI(DBDPlugin plugin, Player survivor, SkillCheckType type) {
        this.plugin = plugin;
        this.player = survivor;
        this.generator = null;
        this.type = type;
        this.killer = null;
        this.inventory = Bukkit.createInventory(this, 54, "§4Struggle!");
        setupZones();
        startRotation();
    }

    private void setupZones() {
        int zoneStart = 15 + new Random().nextInt(15);
        this.greatZoneSlot = zoneStart;
        this.successZoneStart = zoneStart + 1;
        this.successZoneEnd = zoneStart + 2;
        this.currentPosition = 9;
        updateGUI();
    }

    private void updateGUI() {
        inventory.clear();
        ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blackMeta = blackGlass.getItemMeta();
        blackMeta.setDisplayName(" ");
        blackGlass.setItemMeta(blackMeta);

        ItemStack redGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta redMeta = redGlass.getItemMeta();
        redMeta.setDisplayName("§c§lMISS!");
        redGlass.setItemMeta(redMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45) {
                inventory.setItem(i, blackGlass);
            } else {
                inventory.setItem(i, redGlass);
            }
        }

        ItemStack yellowGlass = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta yellowMeta = yellowGlass.getItemMeta();
        yellowMeta.setDisplayName("§6§l★ GREAT!");
        yellowGlass.setItemMeta(yellowMeta);
        inventory.setItem(greatZoneSlot, yellowGlass);

        ItemStack greenGlass = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta greenMeta = greenGlass.getItemMeta();
        greenMeta.setDisplayName("§a§lGOOD");
        greenGlass.setItemMeta(greenMeta);
        inventory.setItem(successZoneStart, greenGlass);
        inventory.setItem(successZoneEnd, greenGlass);

        ItemStack pointer = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta pointerMeta = pointer.getItemMeta();
        pointerMeta.setDisplayName("§f§l▼ KLIKNIJ! ▼");
        pointer.setItemMeta(pointerMeta);
        inventory.setItem(currentPosition, pointer);
    }

    private void startRotation() {
        rotationTask = new BukkitRunnable() {
            public void run() {
                if (!isActive || !player.isOnline()) {
                    this.cancel();
                    isActive = false;
                    return;
                }

                // Jeśli gracz zamknął inventory (wciśnięto ESC) podczas trwania testu, to
                // oblewamy
                if (!player.getOpenInventory().getTopInventory().equals(inventory)) {
                    forceFail(false);
                    return;
                }

                currentPosition++;

                // Porażka, jeśli wskaźnik minie strefę sukcesu bez kliknięcia gracza
                if (currentPosition > successZoneEnd + 2) {
                    forceFail(true);
                    return;
                }

                if (currentPosition >= 45)
                    currentPosition = 9;
                updateGUI();
                if (currentPosition % 3 == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2F, 2.0F);
                }
            }
        };
        long speed = switch (type) {
            case GENERATOR -> 3L;
            case CARRY -> 2L;
            case HOOK_STRUGGLE -> 3L;
        };
        rotationTask.runTaskTimer(plugin, 0L, speed);
    }

    public void forceFail(boolean closeInventory) {
        if (!isActive)
            return;
        isActive = false;
        hasClicked = true;
        if (rotationTask != null)
            rotationTask.cancel();
        handleFail();
        if (closeInventory) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 5L);
        }
    }

    public void handleClick(int slot) {
        if (!isActive || hasClicked)
            return;

        // BLOKADA: Ignoruj kliknięcia na czarne ramki (góra i dół)
        if (slot < 9 || slot >= 45) {
            return; // Nie liczy się jako kliknięcie
        }

        hasClicked = true;
        isActive = false;
        if (rotationTask != null)
            rotationTask.cancel();

        boolean hitGreat = (currentPosition == greatZoneSlot);
        boolean hitGood = (currentPosition >= successZoneStart && currentPosition <= successZoneEnd);

        if (hitGreat) {
            handleSuccess(true);
        } else if (hitGood) {
            handleSuccess(false);
        } else {
            handleFail();
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.closeInventory(), 5L);
    }

    private void handleSuccess(boolean isGreat) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        switch (type) {
            case GENERATOR:
                if (generator != null) {
                    generator.onSkillCheckSuccess(isGreat);
                    // Wiadomość z configu
                    String msg = isGreat ? pl.dbd.DBDPlugin.getMsg("skill-check-great")
                            : pl.dbd.DBDPlugin.getMsg("skill-check-good");
                    player.sendMessage(msg);
                }
                break;
            case CARRY:
                double chance = isGreat ? 0.08 : 0.04;
                if (Math.random() < chance) {
                    plugin.getCarrySystem().stopCarrying(killer);
                    player.sendMessage(pl.dbd.DBDPlugin.getMsg("alwyrwaesi"));
                    if (killer != null)
                        killer.sendMessage(pl.dbd.DBDPlugin.getMsg("cofiarasiwyrwaa"));
                } else {
                    player.sendMessage(isGreat ? "§6GREAT!" : "§aGOOD!");
                }
                break;
            case HOOK_STRUGGLE:
                player.sendMessage(pl.dbd.DBDPlugin.getMsg("aprzeye"));
                break;
        }
    }

    private void handleFail() {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);

        // Wiadomość z configu
        String msg = pl.dbd.DBDPlugin.getMsg("skill-check-miss");
        player.sendMessage(msg);

        switch (type) {
            case GENERATOR:
                if (generator != null)
                    generator.onSkillCheckFail();
                break;
            case HOOK_STRUGGLE:
                player.sendMessage(pl.dbd.DBDPlugin.getMsg("4lzgine"));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getHookListener().forceStage3Death(player);
                }, 10L);
                break;
            case CARRY:
                break;
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}