package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.UUID;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import java.net.URL;

public class Generator {
    private final Location location;
    private double progress;
    private boolean completed;
    private boolean damagedByKiller;
    private ItemDisplay baseDisplay;
    private ItemDisplay auraDisplay;

    public Generator(Location location, Player creator) {
        this.location = location;
        if (creator != null) {
            // Skopiuj yaw gracza by móc go użyć do rotacji (odwracamy o 180 stopni by
            // tekstura była tyłem do gracza)
            this.location.setYaw(creator.getLocation().getYaw() + 180f);
        }
        this.progress = 0;
        this.completed = false;
        this.damagedByKiller = false;
    }

    public void spawnAura() {
        if (baseDisplay != null && baseDisplay.isValid() && auraDisplay != null && auraDisplay.isValid())
            return;

        // Podniesienie modelu (czaszki) tak, aby lepiej pasował dla killera
        Location spawnLoc = location.clone().add(0.5, 1.0, 0.5);
        spawnLoc.setYaw(location.getYaw()); // Przywracamy zapisany kąt
        spawnLoc.setPitch(0f);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            org.bukkit.profile.PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(
                        "http://textures.minecraft.net/texture/f9dc48ba5326a4078d731cb00441e62ba9402400c0d175b9ac734d2d52cf2329"));
            } catch (Exception ignored) {
            }
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        }

        if (baseDisplay == null || !baseDisplay.isValid()) {
            baseDisplay = location.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
                display.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(2f, 2f, 2f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
                display.setItemStack(head);
                display.setViewRange(50f);
                display.setVisibleByDefault(true); // Widoczny dla każdego (ale bez glowing)
                display.setBillboard(ItemDisplay.Billboard.FIXED); // Nie obraca się z graczem
                display.setGlowing(false);
            });
        }

        if (auraDisplay == null || !auraDisplay.isValid()) {
            auraDisplay = location.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
                display.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(2.01f, 2.01f, 2.01f), // Minimalnie większy do uniknięcia z-fightingu
                        new AxisAngle4f(0f, 0f, 0f, 1f)));
                display.setItemStack(head);
                display.setViewRange(50f);
                display.setVisibleByDefault(false); // Widoczne TYLKO dla killera
                display.setBillboard(ItemDisplay.Billboard.FIXED); // Nie obraca się z graczem
                display.setGlowing(true); // Świecenie na 50 kratek
            });
        }

        // Pokazuj tylko killerom (według stanu gry, nie rangi w permission plugins)
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean isKiller = DBDPlugin.getInstance().getGameManager() != null
                    && DBDPlugin.getInstance().getGameManager().isKiller(p);
            if (isKiller) {
                p.showEntity(DBDPlugin.getInstance(), auraDisplay);
            }
        }
    }

    public void checkAura() {
        if (baseDisplay == null || !baseDisplay.isValid() || auraDisplay == null || !auraDisplay.isValid()) {
            removeAura();
            spawnAura();
        } else {
            // Wymuś odświeżanie ukrywania i pokazywania (chroni przed błędami po re-logu
            // lub przeładowaniu chunków)
            for (Player p : Bukkit.getOnlinePlayers()) {
                boolean isKiller = DBDPlugin.getInstance().getGameManager() != null
                        && DBDPlugin.getInstance().getGameManager().isKiller(p);
                if (isKiller) {
                    p.showEntity(DBDPlugin.getInstance(), auraDisplay);
                } else {
                    p.hideEntity(DBDPlugin.getInstance(), auraDisplay);
                }
            }
        }
    }

    public void removeAura() {
        if (baseDisplay != null) {
            baseDisplay.remove();
            baseDisplay = null;
        }
        if (auraDisplay != null) {
            auraDisplay.remove();
            auraDisplay = null;
        }
    }

    public void updateAuraVisibility(Player p, boolean isKiller) {
        if (auraDisplay != null && auraDisplay.isValid()) {
            if (isKiller) {
                p.showEntity(DBDPlugin.getInstance(), auraDisplay);
            } else {
                p.hideEntity(DBDPlugin.getInstance(), auraDisplay);
            }
        }
    }

    // Wywoływane przez SkillCheckGUI po sukcesie
    public void onSkillCheckSuccess(boolean isGreat) {
        // Great daje większy bonus
        double amount = isGreat ? 7.0 : 5.0;

        this.progress += amount;
        if (this.progress > 100)
            this.progress = 100;

        // Dźwięk sukcesu
        location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
        this.damagedByKiller = false; // Reset po skill checku
    }

    // Wywoływane przez SkillCheckGUI po porażce
    public void onSkillCheckFail() {
        // Odejmujemy 5%
        this.progress = Math.max(0, this.progress - 5.0);

        // Czarny pył
        location.getWorld().spawnParticle(
                Particle.DUST,
                location.clone().add(0.5, 1.2, 0.5),
                30, 0.5, 0.5, 0.5,
                new Particle.DustOptions(Color.BLACK, 2.0f));

        // Wybuch
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

        // Powiadomienie killera
        notifyKiller();
        this.damagedByKiller = false; // Reset po skill checku
    }

    public boolean isDamagedByKiller() {
        return damagedByKiller;
    }

    public void setDamagedByKiller(boolean damagedByKiller) {
        this.damagedByKiller = damagedByKiller;
    }

    private void notifyKiller() {
        // Wiadomość z configu
        String explosionMsg = DBDPlugin.getMsg("generator-exploded");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("dbd.killer")) {
                p.sendMessage(explosionMsg);
                p.spawnParticle(Particle.SMOKE, location.clone().add(0.5, 2, 0.5), 20, 0, 0, 0, 0.1);
            }
        }
    }

    public Location getLocation() {
        return location;
    }

    public double getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setProgress(double p) {
        this.progress = p;
    }

    public void setCompleted(boolean c) {
        this.completed = c;
    }
}