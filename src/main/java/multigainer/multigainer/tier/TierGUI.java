package multigainer.multigainer.tier;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class TierGUI {
    public static void open(Player player, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Tier Advancement");

        // Decorative Fill
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // Tier Info Item (Beacon)
        ItemStack stats = new ItemStack(Material.BEACON);
        ItemMeta sm = stats.getItemMeta();
        sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TIER " + profile.getTier());
        sm.setLore(Arrays.asList(
                " ",
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Current Multi: " + ChatColor.YELLOW +
                        String.format("%.0fx", TierManager.getMultiplierForTier(profile.getTier())),
                // ADDED: Displays the user's current accumulated Tier Points directly on the Beacon
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Tier Points: " + ChatColor.LIGHT_PURPLE + profile.getTierPoints(),
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Next Tier Cost: " + ChatColor.AQUA +
                        NumberFormatter.format(new BigNumber(TierManager.getCostForTier(profile.getTier() + 1))) + " Points",
                " "
        ));
        stats.setItemMeta(sm);

        // Metrics calculations
        double currentPoints = profile.getRebirthPoints();
        double nextCost = TierManager.getCostForTier(profile.getTier() + 1);
        boolean canTier = currentPoints >= nextCost;

        String formattedCurrent = NumberFormatter.format(new BigNumber(currentPoints));
        String formattedCost = NumberFormatter.format(new BigNumber(nextCost));

        // Tier Up Button
        ItemStack button = new ItemStack(canTier ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta bm = button.getItemMeta();
        bm.setDisplayName(canTier ? ChatColor.GREEN + "" + ChatColor.BOLD + "TIER UP" : ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
        bm.setLore(Arrays.asList(
                ChatColor.GRAY + "Resets your Money and Upgrades",
                ChatColor.GRAY + "to gain a permanent multiplier boost.",
                " ",
                ChatColor.GRAY + "Progress: " + ChatColor.AQUA + formattedCurrent + ChatColor.GRAY + "/" + ChatColor.GOLD + formattedCost,
                " ",
                canTier ? ChatColor.YELLOW + "Click to advance!" : ChatColor.RED + "Not enough points."
        ));
        button.setItemMeta(bm);

        // Explanatory Guide Book Item
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "TIER GUIDEBOOK");
        bookMeta.setLore(Arrays.asList(
                " ",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "What is Tier Advancement?",
                ChatColor.GRAY + "A milestone which gives you 2x money everytime,",
                ChatColor.GRAY + "Its the last reset layer, essential for progression .",
                " ",
                ChatColor.RED + "" + ChatColor.BOLD + "What you LOSE:",
                ChatColor.GRAY + "● All Money",
                ChatColor.GRAY + "● Upgrades (slot 5)",
                " ",
                ChatColor.GREEN + "" + ChatColor.BOLD + "What you KEEP:",
                ChatColor.GRAY + "● Everything else",
                " "
        ));
        book.setItemMeta(bookMeta);

        inv.setItem(11, stats);
        inv.setItem(13, button);
        inv.setItem(15, book);
        player.openInventory(inv);
    }
}