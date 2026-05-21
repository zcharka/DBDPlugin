package pl.dbd.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.dbd.DBDPlugin;
import pl.dbd.equipment.EquipmentManager;

public class SoulsChequeListener implements Listener {

    private final DBDPlugin plugin;
    private final NamespacedKey chequeKey;

    public SoulsChequeListener(DBDPlugin plugin) {
        this.plugin = plugin;
        this.chequeKey = new NamespacedKey(plugin, "souls_cheque_value");
    }

    @EventHandler
    public void onChequeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        // Sprawdzamy czy papier ma wpisaną w siebie zakodowaną informację
        if (meta.getPersistentDataContainer().has(chequeKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);

            Integer soulsAmount = meta.getPersistentDataContainer().get(chequeKey, PersistentDataType.INTEGER);
            if (soulsAmount == null || soulsAmount <= 0) {
                player.sendMessage(pl.dbd.DBDPlugin.getMsg("clbd7czekjestuszkodz"));
                return;
            }

            // Zabieramy jeden egzemplarz czeku
            item.setAmount(item.getAmount() - 1);

            // Wypłacamy graczowi dusze
            plugin.getSoulsManager().add(player, soulsAmount);

            // Powiadomienie i efekty
            String redeemedMsg = plugin.getConfig().getString("souls-cheque.message-redeemed",
                    "&8▸ &aZrealizowano czek! &7Otrzymujesz &e{amount} &7Dusz Nexusa.");
            player.sendMessage(EquipmentManager.color(redeemedMsg.replace("{amount}", String.valueOf(soulsAmount))));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
        }
    }
}
