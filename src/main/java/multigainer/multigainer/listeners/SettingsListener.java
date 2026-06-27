package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.formatting.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class SettingsListener implements Listener {

    private final Multigainer plugin;
    static final String PDC_KEY = "multigainer_format_settings";

    public SettingsListener(Multigainer plugin) { this.plugin = plugin; }

    /** Builds the slot-8 settings compass reflecting the player's current mode. */
    public ItemStack buildItem(UUID playerId) {
        boolean suffix = NumberFormatter.isSuffix(playerId);
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§8[§7⚙ §fNumber Format§8]");
            meta.setLore(List.of(
                suffix ? "§a● §fSuffix  §8│ §7○ Scientific"
                       : "§7○ Suffix  §8│ §a● §fScientific",
                "§8Right-click to toggle"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_KEY), PersistentDataType.BYTE, (byte) 1
            );
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isSettingsItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.BYTE);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isSettingsItem(event.getItem())) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        NumberFormatter.toggleMode(uid);

        // Refresh the item in the slot to show the new mode
        int slot = player.getInventory().getHeldItemSlot();
        player.getInventory().setItem(slot, buildItem(uid));

        boolean suffix = NumberFormatter.isSuffix(uid);
        player.sendMessage(suffix
            ? "§7Format: §aSuffix §8(K, M, B, T, Qa…)"
            : "§7Format: §bScientific §8(1.23e6, 1.23e331…)");
    }

    // Prevent moving or dropping the settings item into external inventories
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack cur    = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean involves = (cur != null && isSettingsItem(cur))
                        || (cursor != null && isSettingsItem(cursor));
        if (!involves) return;
        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isSettingsItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
