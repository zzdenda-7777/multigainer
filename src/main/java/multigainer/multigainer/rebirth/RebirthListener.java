package multigainer.multigainer.rebirth;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.formatting.NumberFormatter; // ADDED: Imports your suffix formatter
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RebirthListener implements Listener {
    private final Multigainer plugin;

    public RebirthListener(Multigainer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Ensure this listener only interacts with the correct GUI container[cite: 23]
        if (!event.getView().getTitle().equals("§8Rebirth Terminal")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getSlot() != 13) return; // Only process inputs targeted at the main interactive slot[cite: 23]

        Player player = (Player) event.getWhoClicked();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        // Validate player account status against configuration baselines[cite: 23]
        if (profile.getMoney().toDouble() < RebirthManager.REBIRTH_THRESHOLD) {
            player.sendMessage(ChatColor.RED + "You need at least 500,000 money to rebirth!");
            player.closeInventory();
            return;
        }

        // Process reward distributions[cite: 23]
        double pointsGained = RebirthManager.calculateRebirthPoints(profile.getMoney().toDouble());
        profile.setRebirthPoints(profile.getRebirthPoints() + pointsGained);
        profile.setRebirthCount(profile.getRebirthCount() + 1);

        // FIX: Explicitly define both parameters as 0.0 to prevent Math.log10(0) from breaking your income[cite: 23]
        profile.setMoney(new BigNumber(0.0, 0.0));

        // OPTIONAL: Reset upgrade metrics if your game design requires an operational loop wipe[cite: 23]
        profile.setUpgradeLevel(0);

        // CHANGED: Formats the massive unformatted number from image_34ec6c.jpg into a clean formatted suffix string
        String formattedGained = NumberFormatter.format(new BigNumber(pointsGained));

        // Send immersive title/subtitle notification graphics directly onto the player's view screen[cite: 23]
        player.sendTitle(
                ChatColor.GREEN + "" + ChatColor.BOLD + "REBIRTHED",
                ChatColor.AQUA + "You have gained: " + ChatColor.YELLOW + formattedGained + ChatColor.AQUA + " Rebirth Points",
                10, 70, 20
        );

        // Instantly force-refresh the scoreboard UI layout[cite: 23]
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(
                    player,
                    profile.getMoney(),
                    profile.getGems(),
                    profile.getRubies(),
                    profile.getFarmingLevel(),
                    profile.getFarmingXp(),
                    profile.getMiningLevel(),
                    profile.getMiningXp()
            );
        }

        player.sendMessage(ChatColor.GREEN + "✨ You have successfully rebirthed!");
        player.closeInventory();
    }
}