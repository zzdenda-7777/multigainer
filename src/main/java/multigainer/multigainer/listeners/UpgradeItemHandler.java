package multigainer.multigainer.listeners;

import multigainer.multigainer.Multigainer;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public class UpgradeItemHandler implements Listener, CommandExecutor {

    private final Multigainer plugin;
    private final String itemDisplayName = "§a§lUpgrades §7(Right Click)";

    public UpgradeItemHandler(Multigainer plugin) {
        this.plugin = plugin;
    }

    private void openUpgradeMenu(Player player) {
        // Fixed: Passing 'plugin' instead of 'player' to perfectly match your GUI constructor signature
        new multigainer.multigainer.upgrades.UpgradeGUI(plugin);
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
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Index 4 is the 5th inventory slot in the hotbar
        event.getPlayer().getInventory().setItem(4, getUpgradeEmerald());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.EMERALD && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().equals(itemDisplayName)) {
                event.setCancelled(true);
                openUpgradeMenu(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.EMERALD
                && event.getCurrentItem().hasItemMeta()
                && event.getCurrentItem().getItemMeta().getDisplayName().equals(itemDisplayName)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.EMERALD && item.hasItemMeta()
                && item.getItemMeta().getDisplayName().equals(itemDisplayName)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            openUpgradeMenu((Player) sender);
        } else {
            sender.sendMessage("Only players can run this command.");
        }
        return true;
    }
}