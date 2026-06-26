package multigainer.multigainer.upgrades;

import multigainer.multigainer.Multigainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class UpgradeItemHandler implements Listener, CommandExecutor {
    private final Multigainer plugin;
    private final UpgradeGUI upgradeGUI;
    private final String itemDisplayName = "§a§lUpgrades";

    public UpgradeItemHandler(Multigainer plugin) {
        this.plugin = plugin;
        this.upgradeGUI = new UpgradeGUI(plugin);
        Bukkit.getPluginManager().registerEvents(this.upgradeGUI, plugin);
    }

    public ItemStack getUpgradeEmerald() {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta meta = emerald.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(itemDisplayName);
            meta.setLore(Collections.singletonList("§7Click to upgrade your multipliers!"));
            emerald.setItemMeta(meta);
        }
        return emerald;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.isSimilar(getUpgradeEmerald())) {
                event.setCancelled(true);
                upgradeGUI.openGUI(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();
        boolean currentIsEmerald = current != null && current.isSimilar(getUpgradeEmerald());
        boolean cursorIsEmerald  = cursor  != null && cursor.isSimilar(getUpgradeEmerald());
        if (!currentIsEmerald && !cursorIsEmerald) return;
        // Allow free movement within the player's own inventory
        if (event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().isSimilar(getUpgradeEmerald())) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            upgradeGUI.openGUI((Player) sender);
        } else {
            sender.sendMessage("Only players can run this command.");
        }
        return true;
    }
}
