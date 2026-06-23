package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

public class ToolItemHandler implements Listener {
    private final Multigainer plugin;
    private final ToolGUI toolGUI;

    private final String hoeName = "§a§lCustom Wooden Hoe §7(Right Click)";
    private final String pickaxeName = "§b§lCustom Wooden Pickaxe §7(Right Click)";

    public ToolItemHandler(Multigainer plugin) {
        this.plugin = plugin;
        this.toolGUI = new ToolGUI(plugin);
        Bukkit.getPluginManager().registerEvents(this.toolGUI, plugin);
    }

    // Helper method to build the custom wooden hoe
    public ItemStack getCustomHoe() {
        ItemStack hoe = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hoeName);
            meta.setLore(Collections.singletonList("§7Right click to view upgrades!"));
            meta.setUnbreakable(true); // Prevents the tool from breaking while farming
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    // Helper method to build the custom wooden pickaxe
    public ItemStack getCustomPickaxe() {
        ItemStack pickaxe = new ItemStack(Material.WOODEN_PICKAXE);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(pickaxeName);
            meta.setLore(Collections.singletonList("§7Right click to view upgrades!"));
            meta.setUnbreakable(true); // Prevents the tool from breaking while mining
            pickaxe.setItemMeta(meta);
        }
        return pickaxe;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Set custom hoe to slot 0 and custom pickaxe to slot 1
        player.getInventory().setItem(0, getCustomHoe());
        player.getInventory().setItem(1, getCustomPickaxe());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item == null) return;

            // Check if the item is our custom hoe
            if (item.isSimilar(getCustomHoe())) {
                event.setCancelled(true);
                toolGUI.openGUI(event.getPlayer(), ToolGUI.HOE_TITLE);
            }
            // Check if the item is our custom pickaxe
            else if (item.isSimilar(getCustomPickaxe())) {
                event.setCancelled(true);
                toolGUI.openGUI(event.getPlayer(), ToolGUI.PICKAXE_TITLE);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;

        // Prevent players from moving their tools around or losing them in secondary containers
        if (currentItem.isSimilar(getCustomHoe()) || currentItem.isSimilar(getCustomPickaxe())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        // Block the player from dropping their tools on the ground
        if (droppedItem.isSimilar(getCustomHoe()) || droppedItem.isSimilar(getCustomPickaxe())) {
            event.setCancelled(true);
        }
    }
}
