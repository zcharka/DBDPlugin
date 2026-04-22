package pl.dbd.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

public class Hook {
    private final Location location;
    private Player hookedPlayer;
    private int hookStage = 0; // 0 = pusty, 1 = wiszenie, 2 = struggle
    private int antiCampTicks = 0;
    private boolean placedBarrier = false;

    private BlockDisplay auraDisplay;

    public Hook(Location loc) {
        this.location = loc;
        spawnAura();
    }

    public void spawnAura() {
        if (auraDisplay != null && auraDisplay.isValid())
            return;

        // Środek bloku łańcucha
        Location spawnLoc = location.clone().add(0.5, 0.5, 0.5);

        auraDisplay = (BlockDisplay) location.getWorld().spawnEntity(spawnLoc, EntityType.BLOCK_DISPLAY);
        auraDisplay.setBlock(location.getBlock().getBlockData());

        // Ustawienia wizualne aury
        auraDisplay.setVisibleByDefault(false);
        auraDisplay.setGlowing(true);
        auraDisplay.setPersistent(false); // Żeby znikła po restarcie serwera (zostanie stworzona na nowo)
    }

    public void checkAura() {
        if (auraDisplay == null || !auraDisplay.isValid()) {
            auraDisplay = null;
            spawnAura();
        }
    }

    public void removeAura() {
        if (auraDisplay != null) {
            auraDisplay.remove();
            auraDisplay = null;
        }
    }

    public void showAuraToPlayer(Player p) {
        if (auraDisplay != null && auraDisplay.isValid()) {
            p.showEntity(DBDPlugin.getInstance(), auraDisplay);
        }
    }

    public void hideAuraFromPlayer(Player p) {
        if (auraDisplay != null && auraDisplay.isValid()) {
            p.hideEntity(DBDPlugin.getInstance(), auraDisplay);
        }
    }

    public void hookPlayer(Player p) {
        this.hookedPlayer = p;
        this.hookStage = 1;
        p.teleport(location.clone().add(0.5, -0.5, 0.5));

        org.bukkit.block.Block under = location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY() - 3,
                location.getBlockZ());
        if (under.getType() == Material.AIR || under.getType() == Material.CAVE_AIR
                || under.getType() == Material.VOID_AIR) {
            under.setType(Material.BARRIER);
            this.placedBarrier = true;
        }

        pl.dbd.DBDPlugin.getInstance().getGameManager().setPlayerRedGlow(p, true);
        // Team color effect can be established via scoreboard, but glowing is essential
    }

    // Tę metodę wywołujesz w HookManagerze bez argumentów
    public void unhookPlayer() {
        if (this.placedBarrier) {
            org.bukkit.block.Block under = location.getWorld().getBlockAt(location.getBlockX(),
                    location.getBlockY() - 3, location.getBlockZ());
            if (under.getType() == Material.BARRIER) {
                under.setType(Material.AIR);
            }
            this.placedBarrier = false;
        }

        if (this.hookedPlayer != null) {
            pl.dbd.DBDPlugin.getInstance().getGameManager().setPlayerRedGlow(this.hookedPlayer, false);
        }
        this.hookedPlayer = null;
        this.hookStage = 0;
        this.antiCampTicks = 0;
    }

    public void incrementAntiCamp() {
        if (!isOccupied())
            return;

        boolean killerNear = false;
        for (java.util.UUID killerId : pl.dbd.DBDPlugin.getInstance().getGameManager().getKillers()) {
            Player killer = Bukkit.getPlayer(killerId);
            if (killer != null && killer.isOnline() && killer.getLocation().getWorld().equals(location.getWorld())
                    && killer.getLocation().distance(location) < 16) {
                killerNear = true;
                break;
            }
        }

        int required = pl.dbd.DBDPlugin.getInstance().getConfig().getInt("hook.anti-camp-time", 30) * 20;

        if (killerNear) {
            antiCampTicks += 20; // Wołane co sekundę w HookManager
            if (antiCampTicks >= required && antiCampTicks < required + 20) {
                if (hookedPlayer != null) {
                    hookedPlayer.sendTitle("§a§lKAMPING", "§7Wciśnij SHIFT by uciec", 5, 40, 5);
                    hookedPlayer.sendMessage(
                            "§a§l[Anti-Facecamp] §fKiller stoi blisko ciebie! Kliknij kucanie (SHIFT), by samodzielnie uciec z haka!");
                }
            }
        } else {
            // Wygaszanie licznika
            if (antiCampTicks > 0) {
                antiCampTicks -= 10;
            }
        }
    }

    public boolean canSelfUnhook() {
        int required = pl.dbd.DBDPlugin.getInstance().getConfig().getInt("hook.anti-camp-time", 30) * 20;
        return antiCampTicks >= required;
    }

    // Metody, o które krzyczał kompilator:
    public boolean isOccupied() {
        return hookedPlayer != null;
    }

    public int getHookStage() {
        return hookStage;
    }

    public void setHookStage(int stage) {
        this.hookStage = stage;
    }

    public Player getHookedPlayer() {
        return hookedPlayer;
    }

    public Location getLocation() {
        return location;
    }

    public void setHookedPlayer(Player p) {
        this.hookedPlayer = p;
    }
}