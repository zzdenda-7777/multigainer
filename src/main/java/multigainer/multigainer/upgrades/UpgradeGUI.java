package multigainer.multigainer.upgrades;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class UpgradeGUI implements Listener {
    private final Multigainer plugin;
    private final String menuTitle = ChatColor.DARK_GRAY + "Money Multiplier Upgrades";

    public UpgradeGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, menuTitle);
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        int currentLevel = profile.getUpgradeLevel();

        ItemStack upgradeItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = upgradeItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Money Multiplier");
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + currentLevel + "/" + UpgradeManager.MAX_LEVEL);
            lore.add(ChatColor.GRAY + "Total Multiplier: " + ChatColor.GOLD + NumberFormatter.format(UpgradeManager.getTotalMultiplier(currentLevel)) + "x");
            lore.add(" ");

            if (currentLevel < UpgradeManager.MAX_LEVEL) {
                int nextLevel = currentLevel + 1;
                BigNumber cost = UpgradeManager.getUpgradeCost(nextLevel);
                double nextMulti = UpgradeManager.getTierMultiplier(nextLevel);

                lore.add(ChatColor.GRAY + "Next Tier Multiplier: " + ChatColor.AQUA + "x" + nextMulti);
                lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + NumberFormatter.format(cost));
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Click to purchase upgrade!");
            } else {
                lore.add(ChatColor.RED + "✔ Max upgrade tier achieved!");
            }

            meta.setLore(lore);
            upgradeItem.setItemMeta(meta);
        }

        inv.setItem(4, upgradeItem);
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getSlot() != 4) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        int currentLevel = profile.getUpgradeLevel();

        if (currentLevel >= UpgradeManager.MAX_LEVEL) {
            player.sendMessage(ChatColor.RED + "You are already at the maximum upgrade level!");
            return;
        }

        int nextLevel = currentLevel + 1;
        BigNumber cost = UpgradeManager.getUpgradeCost(nextLevel);

        if (profile.getMoney().compareTo(cost) >= 0) {
            profile.setMoney(profile.getMoney().subtract(cost));
            profile.setUpgradeLevel(nextLevel);
            player.sendMessage(ChatColor.GREEN + "✔ Successfully upgraded to Tier " + nextLevel + "!");
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
            openGUI(player);
        } else {
            player.sendMessage(ChatColor.RED + "You need $" + NumberFormatter.format(cost) + " to buy this!");
        }
    }
}