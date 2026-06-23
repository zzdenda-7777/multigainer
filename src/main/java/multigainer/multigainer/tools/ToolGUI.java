package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ToolGUI implements Listener {
    private final Multigainer plugin;

    // Custom titles to differentiate which tool was clicked
    public static final String HOE_TITLE = ChatColor.DARK_GREEN + "Wooden Hoe Menu";
    public static final String PICKAXE_TITLE = ChatColor.DARK_AQUA + "Wooden Pickaxe Menu";

    public ToolGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, String title) {
        // Creates a clean 9-slot inventory menu
        Inventory inv = Bukkit.createInventory(null, 9, title);

        // Currently empty as requested! Ready for future items.

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // If the clicked inventory matches either tool menu, cancel the click interaction
        if (title.equals(HOE_TITLE) || title.equals(PICKAXE_TITLE)) {
            event.setCancelled(true);
        }
    }
}
