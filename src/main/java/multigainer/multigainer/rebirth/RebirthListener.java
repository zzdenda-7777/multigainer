package multigainer.multigainer.rebirth;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RebirthListener implements Listener {
    private final Multigainer plugin;

    public RebirthListener(Multigainer plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Rebirth System")) return;
        event.setCancelled(true); // Stop players from taking items

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getSlot() != 13) return; // Only trigger if they click the middle slot

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getWhoClicked();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        // Check threshold
        if (profile.getMoney().toDouble() < 500000) {
            player.sendMessage(ChatColor.RED + "You need at least 500,000 money to rebirth!");
            player.closeInventory();
            return;
        }

        // Perform Rebirth
        double pointsGained = RebirthManager.calculateRebirthPoints(profile.getMoney().toDouble());
        profile.setRebirthPoints(profile.getRebirthPoints() + pointsGained);
        profile.setRebirthCount(profile.getRebirthCount() + 1);

        // Reset money (Assuming you have a method to set money back to 0)
        profile.setMoney(new multigainer.multigainer.math.BigNumber(0));

        player.sendMessage(ChatColor.GREEN + "✨ You have successfully rebirthed!");
        player.closeInventory();
    }
}
