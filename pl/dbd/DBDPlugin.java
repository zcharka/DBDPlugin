package pl.dbd;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pl.dbd.carry.CarrySystem;
import pl.dbd.game.GameManager;
import pl.dbd.state.PlayerStateManager;
import pl.dbd.managers.PlayerDataManager;
import pl.dbd.util.PAPIExpansion;
import pl.dbd.commands.*;
import pl.dbd.commands.SoulsCommand;
import pl.dbd.economy.SoulsManager;
import pl.dbd.equipment.EquipmentManager;
import pl.dbd.exitgate.ExitGateManager;
import pl.dbd.generator.GeneratorListener;
import pl.dbd.generator.GeneratorManager;
import pl.dbd.hook.HookListener;
import pl.dbd.hook.HookManager;
import pl.dbd.listeners.*;
import pl.dbd.locker.LockerManager;
import pl.dbd.shop.ShopManager;
import pl.dbd.window.WindowManager;
import pl.dbd.window.WindowCommand;
import pl.dbd.shop.ShopAdminGUI;
import pl.dbd.commands.ShopAdminCommand;

public class DBDPlugin extends JavaPlugin {

    private static DBDPlugin instance;

    // --- MANAGERY DANYCH ---
    private PlayerDataManager playerDataManager;
    private SoulsManager soulsManager;
    private PlayerStateManager stateManager;

    // --- MANAGERY MECHANIK ---
    private EquipmentManager equipmentManager;
    private ShopManager shopManager;
    private GeneratorManager generatorManager;
    private HookManager hookManager;
    private LockerManager lockerManager;
    private ExitGateManager exitGateManager;
    private WindowManager windowManager;

    // --- SYSTEMY GRY ---
    private CarrySystem carrySystem;
    private GameManager gameManager;
    private pl.dbd.managers.ChaseManager chaseManager;
    private pl.dbd.hook.HookRewardSystem hookRewardSystem;
    private pl.dbd.managers.RecoveryManager recoveryManager;

    // --- LISTENERS ---
    private HookListener hookListener;
    private EquipmentListener equipmentListener;
    private GeneratorListener generatorListener;
    private SpectateCommand spectateCommand;
    private pl.dbd.combat.HitSystemListener hitSystemListener;

    @Override
    public void onEnable() {
        instance = this;
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        saveConfig();

        // 1. Inicjalizacja Managerów Danych
        this.playerDataManager = new PlayerDataManager(this);
        this.soulsManager = new SoulsManager(this);
        this.stateManager = new PlayerStateManager(this);

        // 2. Inicjalizacja Managerów Mechanik
        this.equipmentManager = new EquipmentManager(this);
        this.shopManager = new ShopManager(this);
        this.generatorManager = new GeneratorManager(this);
        this.hookManager = new HookManager(this);
        this.lockerManager = new LockerManager(this);
        this.exitGateManager = new ExitGateManager(this);
        this.windowManager = new WindowManager(this);

        // 3. Inicjalizacja Systemów Gry
        this.carrySystem = new CarrySystem(this);
        this.chaseManager = new pl.dbd.managers.ChaseManager(this);
        this.recoveryManager = new pl.dbd.managers.RecoveryManager(this);
        this.gameManager = new GameManager(this);
        this.hookRewardSystem = new pl.dbd.hook.HookRewardSystem(this);

        // 4. Inicjalizacja Pól Listenerów
        this.hookListener = new HookListener(this);
        this.equipmentListener = new EquipmentListener(this);
        this.generatorListener = new GeneratorListener(this);

        // 5. REJESTRACJA KOMEND
        registerCommands();

        // 5.5 REJESTRACJA TAB COMPLETERÓW
        registerTabCompleters();

        // 6. REJESTRACJA LISTENERÓW
        registerListeners();

        // 7. PLACEHOLDER API
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        getLogger().info("DBDPlugin włączony poprawnie (Full Version)!");

    }

    private void registerCommands() {
        // --- GAMEPLAY & LOBBY ---
        // Poprawka: ReadyCommand i NotReadyCommand przyjmują teraz 'this' (DBDPlugin)
        safeRegister("gotowy", new ReadyCommand(this));
        safeRegister("niegotowy", new NotReadyCommand(this));

        // GameCommand i GameMapCommand przyjmują GameManager
        safeRegister("game", new GameCommand(gameManager));
        safeRegister("gamemap", new GameMapCommand(gameManager));

        // --- MECHANIKI MAPY ---
        safeRegister("generator", new GeneratorCommand(generatorManager));
        safeRegister("hook", new HookCommand(this, hookManager));
        safeRegister("exitgate", new ExitGateCommand(this));
        safeRegister("locker", new LockerCommand(this));
        safeRegister("window", new WindowCommand(windowManager));

        // --- MEDYCZNE ---
        safeRegister("heal", new HealCommand(this, stateManager));
        safeRegister("healall", new HealAllCommand(this));

        // --- EKONOMIA I GUI ---
        safeRegister("shopadmin", new ShopAdminCommand(this));
        safeRegister("sklep", new ShopCommand(this));
        safeRegister("ekwipunek", new EquipmentCommand(this));
        safeRegister("dusze", new SoulsCommand(this));
        safeRegister("dbd", new DbdCommand(this));
        safeRegister("statsreset", new pl.dbd.commands.StatsResetCommand(this));
        safeRegister("blockeq", new pl.dbd.commands.BlockEqCommand(this));
        safeRegister("top", new pl.dbd.commands.TopCommand(this));

        this.spectateCommand = new SpectateCommand(this, gameManager);
        safeRegister("ogladaj", this.spectateCommand);
        getServer().getPluginManager().registerEvents((org.bukkit.event.Listener) getCommand("ogladaj").getExecutor(),
                this);

        if (getCommand("dbddebug") != null) {
            safeRegister("dbddebug", new DebugCommand(this));
        }

        // Test komendy pełzania
        if (getCommand("trycrawl") != null) {
            safeRegister("trycrawl", new pl.dbd.commands.TryCrawlCommand());
        }
    }

    private void safeRegister(String cmdName, CommandExecutor exec) {
        PluginCommand cmd = getCommand(cmdName);
        if (cmd != null) {
            cmd.setExecutor(exec);
        } else {
            getLogger().warning(
                    "OSTRZEŻENIE: Komenda '" + cmdName + "' jest zdefiniowana w kodzie, ale brakuje jej w plugin.yml!");
        }
    }

    private void registerTabCompleters() {
        pl.dbd.commands.TabCompleterRegistry tabRegistry = new pl.dbd.commands.TabCompleterRegistry(this);
        String[] cmdsWithTab = {
                "game", "gamemap", "generator", "exitgate", "hook", "window", "locker",
                "heal", "healall", "dbd", "statsreset", "blockeq", "ogladaj",
                "gotowy", "niegotowy", "sklep", "shopadmin", "ekwipunekadmin", "trycrawl"
        };
        for (String cmdName : cmdsWithTab) {
            PluginCommand cmd = getCommand(cmdName);
            if (cmd != null) {
                cmd.setTabCompleter(tabRegistry);
            }
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(hookListener, this);
        getServer().getPluginManager().registerEvents(equipmentListener, this);
        getServer().getPluginManager().registerEvents(new pl.dbd.listeners.SoulsChequeListener(this), this);
        // Listener z EQAdmin
        getServer().getPluginManager().registerEvents(new pl.dbd.equipment.EquipmentAdminGUI(this), this);

        // Komenda EQAdmin
        getCommand("ekwipunekadmin").setExecutor((s, c, l, a) -> {
            if (s instanceof org.bukkit.entity.Player p) {
                if (p.hasPermission("dbd.admin")) {
                    new pl.dbd.equipment.EquipmentAdminGUI(this).open(p);
                    return true;
                }
            }
            return false;
        });

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(generatorListener, this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new pl.dbd.exitgate.ExitGateListener(this), this);
        getServer().getPluginManager().registerEvents(carrySystem, this);
        getServer().getPluginManager().registerEvents(new ShopAdminGUI(this), this);
        getServer().getPluginManager().registerEvents(new pl.dbd.shop.ShopGUI(this), this);
        // DODANE: Rejestracja obrażeń na które użytkownik w grze narzekał
        // (HitSystemListener)
        this.hitSystemListener = new pl.dbd.combat.HitSystemListener(this);
        getServer().getPluginManager().registerEvents(this.hitSystemListener, this);
        getServer().getPluginManager().registerEvents(new pl.dbd.protection.UniversalProtectionListener(this), this);

        // REJESTRACJA WINDOW LISTENER
        getServer().getPluginManager().registerEvents(new pl.dbd.window.WindowListener(this, this.windowManager), this);

        // REJESTRACJA MOVEMENT LISTENER (FALL DAMAGE)
        getServer().getPluginManager().registerEvents(new pl.dbd.movement.MovementListener(this), this);

        // REJESTRACJA LOCKER LISTENER (SZAFY)
        getServer().getPluginManager().registerEvents(new pl.dbd.locker.LockerListener(this), this);

        // BLOKADA SKOKU - z wyjątkiem przebywania koło okna
        org.bukkit.scheduler.BukkitRunnable jumpBlockTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (!p.hasPermission("group.default") && !p.hasPermission("group.oglada")) {
                        boolean nearWindow = false;
                        if (windowManager != null) {
                            for (org.bukkit.Location winLoc : windowManager.getAllWindows()) {
                                if (winLoc.getWorld() != null && winLoc.getWorld().equals(p.getWorld())) {
                                    if (winLoc.distance(p.getLocation()) < 2.0) {
                                        org.bukkit.util.Vector toWindow = winLoc.toVector()
                                                .subtract(p.getLocation().toVector()).normalize();
                                        org.bukkit.util.Vector looking = p.getLocation().getDirection().normalize();
                                        if (toWindow.dot(looking) > 0.3) {
                                            nearWindow = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (!nearWindow) {
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                    org.bukkit.potion.PotionEffectType.JUMP_BOOST, 30, 200, false, false, false));
                        } else {
                            if (p.hasPotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST)) {
                                p.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST);
                            }
                        }
                    }
                }
            }
        };
        jumpBlockTask.runTaskTimer(this, 10L, 10L);
    }

    @Override
    public void onDisable() {
        if (generatorManager != null)
            generatorManager.deactivateAll();
        if (exitGateManager != null)
            exitGateManager.deactivateMap();
    }

    // --- GETTERY (WSZYSTKIE) ---
    public static DBDPlugin getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public PlayerStateManager getStateManager() {
        return stateManager;
    }

    public EquipmentManager getEquipmentManager() {
        return equipmentManager;
    }

    public pl.dbd.combat.HitSystemListener getHitSystemListener() {
        return hitSystemListener;
    }

    public pl.dbd.managers.RecoveryManager getRecoveryManager() {
        return recoveryManager;
    }

    public SoulsManager getSoulsManager() {
        return soulsManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public HookManager getHookManager() {
        return hookManager;
    }

    public pl.dbd.managers.ChaseManager getChaseManager() {
        return chaseManager;
    }

    public ExitGateManager getExitGateManager() {
        return exitGateManager;
    }

    public CarrySystem getCarrySystem() {
        return carrySystem;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public LockerManager getLockerManager() {
        return lockerManager;
    }

    public GeneratorListener getGeneratorListener() {
        return generatorListener;
    }

    public WindowManager getWindowManager() {
        return windowManager;
    }

    public HookListener getHookListener() {
        return hookListener;
    }

    public SpectateCommand getSpectateCommand() {
        return spectateCommand;
    }

    public pl.dbd.hook.HookRewardSystem getHookRewardSystem() {
        return hookRewardSystem;
    }

    public EquipmentListener getEquipmentListener() {
        return equipmentListener;
    }

    public static String getMsg(String path, String... placeholders) {
        if (instance == null)
            return path;
        String msg = instance.getConfig().getString("messages." + path);
        if (msg == null) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cBrak: " + path);
        }
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                msg = msg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
    }
}