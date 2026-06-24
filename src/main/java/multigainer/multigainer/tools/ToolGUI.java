package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolGUI implements Listener {
    private final Multigainer plugin;

    public static final String HOE_TITLE = "§2Wooden Hoe Menu";

    public ToolGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void openHoeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, HOE_TITLE);

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(HOE_TITLE)) {
            event.setCancelled(true);
        }
    }
}