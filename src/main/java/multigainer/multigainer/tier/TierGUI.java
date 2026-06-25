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
import org.bukkit.inventory.meta.SkullMeta;

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
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) stats.getItemMeta(); // Změněno na SkullMeta a přetypováno

        if (sm != null) {
            // Nastavení hlavy hráče
            sm.setOwningPlayer(player);
        sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TIER " + profile.getTier());
        sm.setLore(Arrays.asList(
                " ",
                ChatColor.GRAY + "Current Multi: " + ChatColor.YELLOW +
                        String.format("%.0fx", TierManager.getMultiplierForTier(profile.getTier())),
                // ADDED: Displays the user's current accumulated Tier Points directly on the Beacon
                ChatColor.GRAY + "Tier Points: " + ChatColor.LIGHT_PURPLE + profile.getTierPoints(),
                ChatColor.GRAY + "Next Tier Cost: " + ChatColor.AQUA +
                        NumberFormatter.format(new BigNumber(TierManager.getCostForTier(profile.getTier() + 1))) + " Points"
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
                canTier ? ChatColor.YELLOW + "Click to TIER UP!" : ChatColor.RED + "Not enough points."
        ));
        button.setItemMeta(bm);

        // Explanatory Guide Book Item
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
            bookMeta.setDisplayName(ChatColor.WHITE + "");
            bookMeta.setLore(Arrays.asList(
                    " ",
                    ChatColor.YELLOW + "TIER ADVANCEMENT",
                    ChatColor.GRAY + "A milestone that doubles your money.",
                    ChatColor.GRAY + "The final reset layer for progression.",
                    " ",
                    ChatColor.RED + "ON ADVANCEMENT",
                    ChatColor.GRAY + "All money is reset and",
                    ChatColor.GRAY + "pickaxe upgrades are cleared.",
                    " ",
                    ChatColor.GREEN + "WHAT REMAINS",
                    ChatColor.GRAY + "All other progress is saved",
                    ChatColor.GRAY + "and stays with you.",
                    " "
            ));
            book.setItemMeta(bookMeta);

        inv.setItem(11, stats);
        inv.setItem(13, button);
        inv.setItem(15, book);
        player.openInventory(inv);
    }
};}
