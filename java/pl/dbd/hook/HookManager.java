package pl.dbd.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class HookManager {
    private final DBDPlugin plugin;

    // Per-mapa storage (wszystkie haki ze wszystkich map)
    private final Map<String, List<Hook>> mapHooks = new HashMap<>();
    // Aktywne haki (aktualnie załadowane na daną mapę)
    private final List<Hook> hooks = new ArrayList<>();
    private String currentMapName = null;

    private final Map<UUID, Integer> playerHookStages = new HashMap<>();
    private final Map<UUID, Integer> playerHookTicks = new HashMap<>();
    private final File hookFile;
    private FileConfiguration data;

    public HookManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.hookFile = new File(plugin.getDataFolder(), "hooks.yml");
        load();

        // Anti chunk-unload task (co 1 sekundę)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Hook h : hooks) {
                h.checkAura();
                h.incrementAntiCamp();

                if (h.isOccupied()) {
                    Player p = h.getHookedPlayer();
                    if (p != null && p.isOnline()) {
                        UUID uuid = p.getUniqueId();
                        int ticks = playerHookTicks.getOrDefault(uuid, 0) + 20;
                        playerHookTicks.put(uuid, ticks);

                        int stage = playerHookStages.getOrDefault(uuid, 1);

                        if (stage == 1 && ticks >= 1200) {
                            playerHookStages.put(uuid, 2);
                            playerHookTicks.put(uuid, 0);
                            h.setHookStage(2);
                            p.sendMessage("§c§lRozpoczyna się faza 2 (Walka)! Uważaj na Skillchecki!");
                            plugin.getGeneratorListener().startHookStruggleChecks(p);
                        } else if (stage == 2 && ticks >= 1200) {
                            plugin.getHookListener().forceStage3Death(p);
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    // ── LOAD / SAVE ──

    public void load() {
        if (!hookFile.exists()) {
            try {
                hookFile.createNewFile();
            } catch (IOException ignored) {
            }
        }
        data = YamlConfiguration.loadConfiguration(hookFile);
        mapHooks.clear();
        hooks.clear();

        // Nowy format: hooks.<mapName>.<index> = "world,x,y,z"
        if (data.contains("hooks")) {
            for (String mapName : data.getConfigurationSection("hooks").getKeys(false)) {
                List<Hook> list = new ArrayList<>();
                if (data.isConfigurationSection("hooks." + mapName)) {
                    for (String key : data.getConfigurationSection("hooks." + mapName).getKeys(false)) {
                        String locStr = data.getString("hooks." + mapName + "." + key);
                        Location loc = parseLocation(locStr);
                        if (loc != null) {
                            list.add(new Hook(loc));
                        }
                    }
                } else {
                    // Stary format: hooks.<index> = "world,x,y,z" (migracja)
                    String locStr = data.getString("hooks." + mapName);
                    Location loc = parseLocation(locStr);
                    if (loc != null) {
                        // Migruj do mapy "default"
                        mapHooks.computeIfAbsent("default", k -> new ArrayList<>()).add(new Hook(loc));
                    }
                    continue;
                }
                mapHooks.put(mapName, list);
            }
        }

        int total = 0;
        for (List<Hook> list : mapHooks.values())
            total += list.size();
        plugin.getLogger().info("[Hooks] Załadowano haków: " + total + " (map: " + mapHooks.size() + ")");
    }

    public void save() {
        data.set("hooks", null);
        for (Map.Entry<String, List<Hook>> entry : mapHooks.entrySet()) {
            String mapName = entry.getKey();
            List<Hook> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                Location loc = list.get(i).getLocation();
                String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                        + loc.getBlockZ();
                data.set("hooks." + mapName + "." + i, locStr);
            }
        }
        try {
            data.save(hookFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Location parseLocation(String str) {
        if (str == null || str.isEmpty())
            return null;
        String[] parts = str.split(",");
        if (parts.length != 4)
            return null;
        try {
            return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]));
        } catch (Exception e) {
            return null;
        }
    }

    // ── PER-MAP AKTYWACJA ──

    public void activateMap(String mapName) {
        // Wyczyść aury starych aktywnych haków
        for (Hook h : hooks) {
            h.removeAura();
        }
        hooks.clear();
        currentMapName = mapName;

        if (mapHooks.containsKey(mapName)) {
            for (Hook h : mapHooks.get(mapName)) {
                h.setHookStage(0);
                hooks.add(h);
            }
            plugin.getLogger().info("[Hooks] Aktywowano haki dla mapy: " + mapName + " (" + hooks.size() + ")");
        } else {
            plugin.getLogger().warning("[Hooks] Brak haków dla mapy: " + mapName);
        }
    }

    public void deactivateMap() {
        for (Hook h : hooks) {
            h.removeAura();
            h.unhookPlayer();
        }
        hooks.clear();
        currentMapName = null;
    }

    // ── TWORZENIE / USUWANIE ──

    /**
     * Tworzy hak na danym bloku dla konkretnej mapy.
     * ZABEZPIECZENIE: jeśli na tym samym bloku jest już hak dla tej mapy,
     * metoda zwraca false i nic nie zapisuje.
     *
     * @return true jeśli hak został utworzony, false jeśli już istniał na tym bloku
     */
    public boolean createHook(Location loc, String mapName) {
        if (loc == null || loc.getWorld() == null || mapName == null)
            return false;

        List<Hook> list = mapHooks.computeIfAbsent(mapName, k -> new ArrayList<>());

        // Blokada duplikatu: ten sam świat i te same koordy blokowe
        for (Hook existing : list) {
            Location exLoc = existing.getLocation();
            if (exLoc != null
                    && exLoc.getWorld() != null
                    && exLoc.getWorld().equals(loc.getWorld())
                    && exLoc.getBlockX() == loc.getBlockX()
                    && exLoc.getBlockY() == loc.getBlockY()
                    && exLoc.getBlockZ() == loc.getBlockZ()) {
                return false;
            }
        }

        Hook newHook = new Hook(loc);
        list.add(newHook);

        // Jeśli ta mapa jest aktywna, dodaj do aktywnych
        if (mapName.equalsIgnoreCase(currentMapName)) {
            hooks.add(newHook);
        }

        save();
        return true;
    }

    // Stara metoda (kompatybilność) — dodaje do mapy "default"
    public boolean createHook(Location loc) {
        return createHook(loc, currentMapName != null ? currentMapName : "default");
    }

    public Hook getHookAt(Location loc) {
        if (loc == null || loc.getWorld() == null)
            return null;
        for (Hook h : hooks) {
            Location hookLoc = h.getLocation();
            if (hookLoc != null && hookLoc.getWorld() != null && hookLoc.getWorld().equals(loc.getWorld())) {
                if (hookLoc.distance(loc) < 3.5)
                    return h;
            }
        }
        return null;
    }

    public boolean removeHook(Location loc) {
        Hook h = getHookAt(loc);
        if (h != null) {
            h.removeAura();
            hooks.remove(h);
            // Usuń z mapHooks
            for (List<Hook> list : mapHooks.values()) {
                list.remove(h);
            }
            save();
            return true;
        }
        return false;
    }

    public Hook getHookByPlayer(Player p) {
        for (Hook h : hooks) {
            if (h.getHookedPlayer() != null && h.getHookedPlayer().getUniqueId().equals(p.getUniqueId())) {
                return h;
            }
        }
        return null;
    }

    public void resetAllHooks() {
        for (Hook h : hooks) {
            h.unhookPlayer();
        }
        playerHookStages.clear();
        playerHookTicks.clear();
    }

    public void resetPlayerHookCount(UUID uuid) {
        playerHookStages.remove(uuid);
        playerHookTicks.remove(uuid);
    }

    public List<Hook> getAllHooks() {
        return new ArrayList<>(hooks);
    }

    // Zwraca nazwy wszystkich map z hakami (do tab completera)
    public java.util.Set<String> getMapNames() {
        return mapHooks.keySet();
    }

    public boolean isPlayerHooked(UUID uuid) {
        for (Hook h : hooks) {
            if (h.getHookedPlayer() != null && h.getHookedPlayer().getUniqueId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public void hookPlayer(Player p, Hook hook) {
        if (hook.isOccupied())
            return;

        UUID uuid = p.getUniqueId();
        int currentStage = playerHookStages.getOrDefault(uuid, 0);
        int nextStage = currentStage + 1;

        if (nextStage >= 3) {
            plugin.getHookListener().forceStage3Death(p);
            return;
        }

        playerHookStages.put(uuid, nextStage);
        playerHookTicks.put(uuid, 0);

        hook.hookPlayer(p);
        hook.setHookStage(nextStage);

        plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.HOOKED);
        p.sendMessage(pl.dbd.DBDPlugin.getMsg("czostaepowieszonynah"));

        if (nextStage == 2) {
            p.sendMessage("§c§lRozpoczyna się faza 2 (Walka)! Uważaj na Skillchecki!");
            plugin.getGeneratorListener().startHookStruggleChecks(p);
        }
    }

    public void showHooksToKiller(Player killer) {
        for (Hook h : hooks) {
            h.showAuraToPlayer(killer);
        }
    }

    public void hideHooksFromKiller(Player killer) {
        for (Hook h : hooks) {
            h.hideAuraFromPlayer(killer);
        }
    }

    public void unhookPlayer(Player p) {
        unhookPlayer(p, null);
    }

    public void unhookPlayer(Player p, Player rescuer) {
        for (Hook h : hooks) {
            if (h.getHookedPlayer() != null && h.getHookedPlayer().getUniqueId().equals(p.getUniqueId())) {
                h.unhookPlayer();
                // NIE resetuj stanu jeśli gracz jest MARTWY (umarł na haku)
                if (plugin.getStateManager().getState(p) != pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
                    plugin.getStateManager().setState(p, pl.dbd.state.PlayerStateManager.PlayerState.INJURED);
                }
                plugin.getGeneratorListener().stopHookStruggleChecks(p);

                if (rescuer != null && !rescuer.getUniqueId().equals(p.getUniqueId())) {
                    String msgRescued = plugin.getConfig()
                            .getString("messages.hook-rescued-by", "§aZostałeś uratowany przez: §e{player}")
                            .replace("{player}", rescuer.getName());
                    String msgRescuer = plugin.getConfig()
                            .getString("messages.hook-rescued", "§aUratowałeś gracza: §e{player}")
                            .replace("{player}", p.getName());
                    p.sendMessage(msgRescued);
                    rescuer.sendMessage(msgRescuer);

                    if (plugin.getHookRewardSystem() != null) {
                        plugin.getHookRewardSystem().rewardUnhooked(rescuer, p);
                    }
                } else if (plugin.getStateManager().getState(p) != pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
                    p.sendMessage(pl.dbd.DBDPlugin.getMsg("azostaezdjtyzhaka"));
                }

                if (plugin.getStateManager().getState(p) != pl.dbd.state.PlayerStateManager.PlayerState.DEAD) {
                    p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                }
                return;
            }
        }
    }
}