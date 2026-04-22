package pl.dbd;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

// Importy Systemów
import pl.dbd.carry.CarrySystem;
import pl.dbd.game.GameManager;
import pl.dbd.state.PlayerStateManager;
import pl.dbd.managers.AfkManager;
import pl.dbd.managers.PlayerDataManager;
import pl.dbd.util.PAPIExpansion;

// Importy Komend
import pl.dbd.commands.*;

// Importy Managerów Mechanik
import pl.dbd.economy.SoulsCommand;
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

public class DBDPlugin extends JavaPlugin {

    private static DBDPlugin instance;

    // --- MANAGERY DANYCH ---
    private PlayerDataManager playerDataManager;
    private SoulsManager soulsManager;
    private PlayerStateManager stateManager;
    private AfkManager afkManager;
    
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

    // --- LISTENERY ---
    private HookListener hookListener;
    private EquipmentListener equipmentListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Inicjalizacja Managerów Danych
        this.playerDataManager = new PlayerDataManager(this);
        this.soulsManager = new SoulsManager(this);
        this.stateManager = new PlayerStateManager(this);
        this.afkManager = new AfkManager(this);

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
        this.gameManager = new GameManager(this);

        // 4. Inicjalizacja Pól Listenerów
        this.hookListener = new HookListener(this);
        this.equipmentListener = new EquipmentListener(this);

        // 5. REJESTRACJA KOMEND
        registerCommands();
        
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
        safeRegister("dusze", new SoulsCommand(this));
        safeRegister("sklep", new ShopCommand(this));
        safeRegister("ekwipunek", new EquipmentCommand(this));

        // --- INNE / DEBUG ---
        if (getCommand("ogladaj") != null) {
            getCommand("ogladaj").setExecutor((sender, cmd, label, args) -> {
                sender.sendMessage("§eTryb obserwatora: §7(Wkrótce)");
                return true;
            });
        }

        if (getCommand("dbddebug") != null) {
            safeRegister("dbddebug", new DebugCommand(this));
        }
    }

    private void safeRegister(String cmdName, CommandExecutor exec) {
        PluginCommand cmd = getCommand(cmdName);
        if (cmd != null) {
            cmd.setExecutor(exec);
        } else {
            getLogger().warning("OSTRZEŻENIE: Komenda '" + cmdName + "' jest zdefiniowana w kodzie, ale brakuje jej w plugin.yml!");
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(hookListener, this);
        getServer().getPluginManager().registerEvents(equipmentListener, this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new ExitGateListener(this), this);
        getServer().getPluginManager().registerEvents(carrySystem, this);
    }

    @Override
    public void onDisable() {
        if (generatorManager != null) generatorManager.deactivateAll();
    }

    // --- GETTERY (WSZYSTKIE) ---
    public static DBDPlugin getInstance() { return instance; }
    
    public GameManager getGameManager() { return gameManager; }
    public GeneratorManager getGeneratorManager() { return generatorManager; }
    public PlayerStateManager getStateManager() { return stateManager; }
    public EquipmentManager getEquipmentManager() { return equipmentManager; }
    public SoulsManager getSoulsManager() { return soulsManager; }
    public ShopManager getShopManager() { return shopManager; }
    public HookManager getHookManager() { return hookManager; }
    public ExitGateManager getExitGateManager() { return exitGateManager; }
    public CarrySystem getCarrySystem() { return carrySystem; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public LockerManager getLockerManager() { return lockerManager; }
    public AfkManager getAfkManager() { return afkManager; }
    public WindowManager getWindowManager() { return windowManager; }
    
    public HookListener getHookListener() { return hookListener; }
    public EquipmentListener getEquipmentListener() { return equipmentListener; }
    
    public String getMessage(String path) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages." + path, "&cBrak: " + path));
    }
}