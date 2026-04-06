package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectateCommand implements CommandExecutor, Listener {
    private final DBDPlugin plugin;
    private final GameManager gameManager;
    private final Map<UUID, org.bukkit.Location> savedLocations = new HashMap<>();

    private final Map<UUID, UUID> spectatingTargets = new HashMap<>();

    public SpectateCommand(DBDPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        // Teleportuj widza za cel co 10 ticków (trzecia osoba)
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : new HashMap<>(spectatingTargets).entrySet()) {
                Player spectator = Bukkit.getPlayer(entry.getKey());
                Player target = Bukkit.getPlayer(entry.getValue());

                if (spectator != null && target != null && spectator.isOnline() && target.isOnline()) {
                    if (spectator.getGameMode() != GameMode.SPECTATOR) {
                        spectator.setGameMode(GameMode.SPECTATOR);
                    }
                    // Nie teleportuj jeśli ma otwarte GUI (np. /ogladaj menu)
                    if (spectator.getOpenInventory().getTopInventory().getSize() > 0
                            && !spectator.getOpenInventory().getTitle().equals("Crafting")) {
                        continue;
                    }
                    // Trzecia osoba: teleportuj kilka bloków za cel
                    org.bukkit.Location targetLoc = target.getLocation();
                    org.bukkit.util.Vector direction = targetLoc.getDirection().normalize().multiply(-3);
                    org.bukkit.Location behindTarget = targetLoc.clone().add(direction).add(0, 2, 0);
                    behindTarget.setDirection(targetLoc.toVector().subtract(behindTarget.toVector()).normalize());
                    spectator.teleport(behindTarget);
                } else if (spectator != null) {
                    stopSpectating(spectator);
                }
            }
        }, 10L, 10L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ctylkogracz"));
            return true;
        }

        if (gameManager.getGameState() != pl.dbd.game.GameManager.GameState.IN_GAME) {
            player.sendMessage("§cGra jeszcze się nie rozpoczęła!");
            return true;
        }

        if (gameManager.isInGame(player) && !gameManager.isDead(player) && !gameManager.hasEscaped(player)) {
            player.sendMessage(pl.dbd.DBDPlugin.getMsg("cniemoeszogldayjeszi"));
            return true;
        }

        openSpectateGUI(player);
        return true;
    }

    private void openSpectateGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "§8Obserwuj: Ocalali");
        int slot = 0;

        for (UUID uuid : gameManager.getSurvivorUUIDs()) {
            Player survivor = Bukkit.getPlayer(uuid);
            if (survivor != null && survivor.isOnline() && !gameManager.isDead(survivor)
                    && !gameManager.hasEscaped(survivor)) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(survivor);
                meta.setDisplayName("§a" + survivor.getName());

                String stateFormat = "§aZdrowy";
                pl.dbd.state.PlayerStateManager.PlayerState state = plugin.getStateManager().getState(survivor);
                if (state == pl.dbd.state.PlayerStateManager.PlayerState.HOOKED)
                    stateFormat = "§cNa Haku";
                else if (state == pl.dbd.state.PlayerStateManager.PlayerState.CARRIED)
                    stateFormat = "§6Niesiony";
                else if (state == pl.dbd.state.PlayerStateManager.PlayerState.DOWNED)
                    stateFormat = "§4Powalony";
                else if (state == pl.dbd.state.PlayerStateManager.PlayerState.INJURED)
                    stateFormat = "§eRanny";
                else if (state == pl.dbd.state.PlayerStateManager.PlayerState.DEAD)
                    stateFormat = "§8Martwy";

                meta.setLore(java.util.List.of("§7Status: " + stateFormat, "", "§7Kliknij, aby obserwować!"));
                head.setItemMeta(meta);
                inv.setItem(slot++, head);
            }
        }

        inv.setItem(8, createItem(Material.BARRIER, "§cPrzestań obserwować"));
        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8Obserwuj: Ocalali"))
            return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;
        Player spectator = (Player) event.getWhoClicked();

        if (event.getSlot() == 8) {
            stopSpectating(spectator);
            spectator.closeInventory();
            return;
        }

        if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            String targetName = org.bukkit.ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
            Player target = Bukkit.getPlayer(targetName);

            if (target != null && target.isOnline()) {
                startSpectating(spectator, target);
                spectator.closeInventory();
            } else {
                spectator.sendMessage(pl.dbd.DBDPlugin.getMsg("ctengraczniejestjudo"));
            }
        }
    }

    private void startSpectating(Player spectator, Player target) {
        if (!savedLocations.containsKey(spectator.getUniqueId())) {
            savedLocations.put(spectator.getUniqueId(), spectator.getLocation());
        }
        spectatingTargets.put(spectator.getUniqueId(), target.getUniqueId());
        spectator.setGameMode(GameMode.SPECTATOR);
        // Trzecia osoba: teleportuj za cel
        org.bukkit.Location targetLoc = target.getLocation();
        org.bukkit.util.Vector direction = targetLoc.getDirection().normalize().multiply(-3);
        org.bukkit.Location behindTarget = targetLoc.clone().add(direction).add(0, 2, 0);
        behindTarget.setDirection(targetLoc.toVector().subtract(behindTarget.toVector()).normalize());
        spectator.teleport(behindTarget);
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + spectator.getName() + " parent set oglada");
        }
        spectator.sendMessage("§a§lObserwujesz: §e" + target.getName());
    }

    private void stopSpectating(Player spectator) {
        spectatingTargets.remove(spectator.getUniqueId());
        org.bukkit.Location loc = savedLocations.remove(spectator.getUniqueId());
        if (loc != null) {
            spectator.teleport(loc);
        } else {
            org.bukkit.Location lobby = gameManager.getLobbySpawn();
            if (lobby != null)
                spectator.teleport(lobby);
        }
        spectator.setGameMode(GameMode.ADVENTURE);
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + spectator.getName() + " parent set default");
        }
        spectator.sendMessage(pl.dbd.DBDPlugin.getMsg("eprzestaeobserwowa"));
    }

    /**
     * Zatrzymuje oglądanie u WSZYSTKICH widzów i teleportuje ich na spawn.
     * Wywoływane przez GameManager.endGame().
     */
    public void stopAllSpectating() {
        for (UUID uuid : new java.util.HashSet<>(spectatingTargets.keySet())) {
            Player spectator = Bukkit.getPlayer(uuid);
            if (spectator != null && spectator.isOnline()) {
                stopSpectating(spectator);
            }
        }
        spectatingTargets.clear();
        savedLocations.clear();
    }
}