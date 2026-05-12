package pl.dbd.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;
import pl.dbd.equipment.EquipmentManager;

import java.util.*;
import java.util.stream.Collectors;

public class SoulsCommand implements TabExecutor {
    private final DBDPlugin plugin;

    public SoulsCommand(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // --- SUBKOMENDA: TOP ---
        if (args.length >= 1 && args[0].equalsIgnoreCase("top")) {
            showTop(sender);
            return true;
        }

        // --- UPRAWNIENIA ADMINA DLA ZARZĄDZANIA DUSZAMI ---
        if (!sender.hasPermission("dbd.admin")) {
            if (args.length == 0) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("msg"));
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("blduszenexusa"));
                sender.sendMessage("");
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszetop"));
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("msg"));
                return true;
            } else {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7niemaszdostpudo"));
            }
            return true;
        }

        // --- KOMENDY ADMINA ---
        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Osobna logika dla /dusze czek [ilość] (nie wymaga targetu gracza)
        if (sub.equals("czek")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(pl.dbd.DBDPlugin.getMsg("ckomendadostpnatylko"));
                return true;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7ilomusibyliczbc"));
                return true;
            }

            if (amount <= 0) {
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7ilomusibywiksza"));
                return true;
            }

            if (!plugin.getSoulsManager().take(p, amount)) {
                p.sendMessage(EquipmentManager.color("&cNie masz wystarczającej ilości Dusz Nexusa!"));
                return true;
            }

            org.bukkit.inventory.ItemStack cheque = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PAPER);
            org.bukkit.inventory.meta.ItemMeta meta = cheque.getItemMeta();
            if (meta != null) {
                String chequeName = plugin.getConfig().getString("souls-cheque.name", "&e&lCzek na Dusze Nexusa");
                meta.setDisplayName(EquipmentManager.color(chequeName));

                List<String> rawLore = plugin.getConfig().getStringList("souls-cheque.lore");
                if (rawLore.isEmpty()) {
                    rawLore = Arrays.asList(
                            "&7Wartość: &e{amount}",
                            "",
                            "&8▸ &7Kliknij &fPPM&7, aby zrealizować.");
                }

                List<String> lore = new java.util.ArrayList<>();
                for (String line : rawLore) {
                    lore.add(EquipmentManager.color(line.replace("{amount}", String.valueOf(amount))));
                }

                meta.setLore(lore);

                org.bukkit.NamespacedKey chequeKey = new org.bukkit.NamespacedKey(plugin, "souls_cheque_value");
                meta.getPersistentDataContainer().set(chequeKey, org.bukkit.persistence.PersistentDataType.INTEGER,
                        amount);

                cheque.setItemMeta(meta);
            }
            p.getInventory().addItem(cheque);
            String createdMsg = plugin.getConfig().getString("souls-cheque.message-created",
                    "&8▸ &aStworzono czek na &e{amount} &7Dusz Nexusa.");
            p.sendMessage(EquipmentManager.color(createdMsg.replace("{amount}", String.valueOf(amount))));
            return true;
        }

        // Pozostałe komendy: dodaj, zabierz, ustaw (wymagają 3 argumentów: sub, gracz,
        // ilość)
        if (args.length < 3) {
            sendHelp(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7graczmusibynase"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7ilomusibyliczbc"));
            return true;
        }

        StringBuilder reason = new StringBuilder();
        if (args.length > 3) {
            for (int i = 3; i < args.length; i++) {
                reason.append(args[i]).append(" ");
            }
        }
        String finalReason = reason.toString().trim();

        switch (sub) {
            case "mnoznik":
                if (args.length < 3) {
                    sender.sendMessage("§cUżycie: /dusze mnoznik <survivor|killer> <wartość>");
                    return true;
                }
                String role = args[1].toLowerCase();
                double multi;
                try {
                    multi = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cWartość musi być liczbą!");
                    return true;
                }
                if (role.equals("survivor") || role.equals("ocalały") || role.equals("ocalaly")) {
                    plugin.getConfig().set("souls-rewards.multiplier-survivor", multi);
                    plugin.saveConfig();
                    sender.sendMessage("§aUstawiono mnożnik Dusz Nexusa dla Ocalałych na " + multi);
                } else if (role.equals("killer") || role.equals("zabójca") || role.equals("zabojca")) {
                    plugin.getConfig().set("souls-rewards.multiplier-killer", multi);
                    plugin.saveConfig();
                    sender.sendMessage("§aUstawiono mnożnik Dusz Nexusa dla Zabójcy na " + multi);
                } else {
                    sender.sendMessage("§cWybierz poprawną rolę: survivor albo killer");
                }
                return true;

            case "dodaj":
                double multiplier = 1.0;
                if (plugin.getGameManager().isSurvivor(target)) {
                    multiplier = plugin.getConfig().getDouble("souls-rewards.multiplier-survivor", 1.0);
                } else if (plugin.getGameManager().isKiller(target)) {
                    multiplier = plugin.getConfig().getDouble("souls-rewards.multiplier-killer", 1.0);
                }

                int rawAmount = amount;
                amount = (int) Math.round(amount * multiplier);

                plugin.getSoulsManager().add(target, amount);
                if (!finalReason.isEmpty()) {
                    sender.sendMessage(
                            EquipmentManager.color("&8▸ &7Dodano &e" + amount + " &7(" + rawAmount + " x " + multiplier
                                    + ") dusz za: &f" + finalReason));
                }
                break;
            case "zabierz":
                boolean success = plugin.getSoulsManager().take(target, amount);
                if (success) {
                    sender.sendMessage(EquipmentManager
                            .color("&8▸ &aPomyślnie zabrano &e" + amount + " &7dusz graczowi &f" + target.getName()));
                } else {
                    sender.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7graczniematylud"));
                }
                break;
            case "ustaw":
                plugin.getSoulsManager().setBalance(target, amount);
                sender.sendMessage(EquipmentManager
                        .color("&8▸ &7Ustawiono balans gracza &f" + target.getName() + " &7na &e" + amount));
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("dbd.admin")) {
            if (args.length == 1) {
                return Arrays.asList("top").stream().filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Arrays.asList("dodaj", "zabierz", "ustaw", "top", "czek", "mnoznik").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("czek")) {
            return Arrays.asList("50", "100", "500").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("mnoznik")) {
            return Arrays.asList("survivor", "killer").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("top")) {
            return null; // Return null to fallback to Bukkit's default online players list
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("top") && !args[0].equalsIgnoreCase("czek")
                && !args[0].equalsIgnoreCase("mnoznik")) {
            return Arrays.asList("10", "50", "100", "500", "1000").stream()
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("msg"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("blduszenexusa87admin"));
        s.sendMessage("");
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszedodajgraczilo"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszezabierzgraczi"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszeustawgraczilo"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszetop"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("8eduszeczekilo"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("msg"));
    }

    private void showTop(CommandSender s) {
        s.sendMessage("");
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("bltop10dusznexusa"));
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("msg1"));

        // Pobieramy dane z Twojego SoulsManagera
        Map<String, Integer> top = plugin.getSoulsManager().getTopPlayers(10);

        if (top.isEmpty()) {
            s.sendMessage(pl.dbd.DBDPlugin.getMsg("7rankingjestobecniep"));
        } else {
            int rank = 1;
            for (Map.Entry<String, Integer> entry : top.entrySet()) {
                s.sendMessage(
                        EquipmentManager.color("&b" + rank + ". &f" + entry.getKey() + " &8▸ &e" + entry.getValue()));
                rank++;
            }
        }
        s.sendMessage(pl.dbd.DBDPlugin.getMsg("msg1"));
    }
}