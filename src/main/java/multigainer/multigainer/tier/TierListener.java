package multigainer.multigainer.tier;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TierListener implements Listener {
    private final Multigainer plugin;

    public TierListener(Multigainer plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§8Tier Advancement")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getSlot() != 13) return;

        Player player = (Player) event.getWhoClicked();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        if (profile == null) return;

        BigNumber cost = TierManager.getCostForTierBig(profile.getTier() + 1);

        if (new BigNumber(profile.getRebirthPoints()).compareTo(cost) < 0) {
            player.sendMessage(ChatColor.RED + "You need " + NumberFormatter.format(cost) + " Rebirth Points to Tier Up!");
            player.closeInventory();
            return;
        }

        // Apply Tier Up with player context passed for title packet targets
        TierManager.performTierUp(profile, player);
        player.sendMessage(ChatColor.GREEN + "✨ You have advanced to Tier " + profile.getTier() + "!");
        player.closeInventory();
    }
}