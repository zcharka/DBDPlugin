package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.dbd.DBDPlugin;
import pl.dbd.game.GameManager;

import java.util.UUID;

public class SpectateCommand implements CommandExecutor, Listener {
    private final DBDPlugin plugin;
    private final GameManager gameManager;

    public SpectateCommand(DBDPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTylko gracz!");
            return true;
        }

        if (gameManager.isInGame(player) && !gameManager.isDead(player) && !gameManager.hasEscaped(player)) {
            player.sendMessage("§cNie możesz oglądać – żyjesz i jesteś w grze!");
            return true;
        }

        openSpectateGUI(player);
        return true;
    }

    private void openSpectateGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Obserwuj: Ocalali");
        int slot = 0;

        for (UUID uuid : gameManager.getSurvivorUUIDs()) {
            Player survivor = Bukkit.getPlayer(uuid);
            if (survivor != null && survivor.isOnline() && !gameManager.isDead(survivor) && !gameManager.hasEscaped(survivor)) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                meta.setOwningPlayer(survivor);
                meta.setDisplayName("§a" + survivor.getName());
                meta.setLore(java.util.List.of("§7Kliknij, aby obserwować!"));
                head.setItemMeta(meta);
                inv.setItem(slot++, head);
            }
        }

        inv.setItem(26, createItem(Material.BARRIER, "§cPrzestań obserwować"));
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
        if (!event.getView().getTitle().equals("§8Obserwuj: Ocalali")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        Player spectator = (Player) event.getWhoClicked();

        if (event.getSlot() == 26) {
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
                spectator.sendMessage("§cTen gracz nie jest już dostępny.");
            }
        }
    }

    private void startSpectating(Player spectator, Player target) {
        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(target.getLocation());
        spectator.setSpectatorTarget(target);
        spectator.sendMessage("§a§lObserwujesz: §e" + target.getName());
    }

    private void stopSpectating(Player spectator) {
        spectator.setSpectatorTarget((Entity) null);
        org.bukkit.Location lobby = gameManager.getLobbySpawn();
        if (lobby != null) spectator.teleport(lobby);
        spectator.setGameMode(GameMode.ADVENTURE);
        spectator.sendMessage("§ePrzestałeś obserwować.");
    }
}