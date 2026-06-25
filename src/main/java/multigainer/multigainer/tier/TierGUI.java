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
        if (profile == null) {
            player.sendMessage("§cProfile not loaded yet. Please wait a moment and try again.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§8Tier Advancement");

        // Border panes
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        int tier         = profile.getTier();
        double tierMulti = TierManager.getMultiplierForTier(tier);
        double nextCost  = TierManager.getCostForTier(tier + 1);
        double curPoints = profile.getRebirthPoints();
        boolean canTier  = curPoints >= nextCost;

        // ── Slot 11: Player head with stats ──────────────────────────────────
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm    = (SkullMeta) stats.getItemMeta();
        if (sm != null) {
            sm.setOwningPlayer(player);
            sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TIER " + tier);
            sm.setLore(Arrays.asList(
                    " ",
                    ChatColor.GRAY + "Current Multi: " + ChatColor.YELLOW + String.format("%.0fx", tierMulti),
                    ChatColor.GRAY + "Tier Points: "   + ChatColor.LIGHT_PURPLE + profile.getTierPoints(),
                    ChatColor.GRAY + "Next Tier Cost: " + ChatColor.AQUA
                            + NumberFormatter.format(new BigNumber(nextCost)) + " Points"
            ));
            stats.setItemMeta(sm);
        }
        inv.setItem(11, stats);

        // ── Slot 13: Tier up button ───────────────────────────────────────────
        ItemStack button = new ItemStack(canTier ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta  bm     = button.getItemMeta();
        if (bm != null) {
            bm.setDisplayName(canTier
                    ? ChatColor.GREEN + "" + ChatColor.BOLD + "TIER UP"
                    : ChatColor.RED   + "" + ChatColor.BOLD + "LOCKED");
            bm.setLore(Arrays.asList(
                    ChatColor.GRAY + "Resets your Money and Upgrades",
                    ChatColor.GRAY + "to gain a permanent multiplier boost.",
                    " ",
                    ChatColor.GRAY + "Progress: " + ChatColor.AQUA
                            + NumberFormatter.format(new BigNumber(curPoints))
                            + ChatColor.GRAY + "/" + ChatColor.GOLD
                            + NumberFormatter.format(new BigNumber(nextCost)),
                    " ",
                    canTier ? ChatColor.YELLOW + "Click to TIER UP!" : ChatColor.RED + "Not enough points."
            ));
            button.setItemMeta(bm);
        }
        inv.setItem(13, button);

        // ── Slot 15: Guide book ───────────────────────────────────────────────
        ItemStack book     = new ItemStack(Material.BOOK);
        ItemMeta  bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.WHITE + "Tier Guide");
            bookMeta.setLore(Arrays.asList(
                    " ",
                    ChatColor.YELLOW + "TIER ADVANCEMENT",
                    ChatColor.GRAY   + "A milestone that doubles your money.",
                    ChatColor.GRAY   + "The final reset layer for progression.",
                    " ",
                    ChatColor.RED  + "ON ADVANCEMENT",
                    ChatColor.GRAY + "All money is reset and",
                    ChatColor.GRAY + "pickaxe upgrades are cleared.",
                    " ",
                    ChatColor.GREEN + "WHAT REMAINS",
                    ChatColor.GRAY  + "All other progress is saved.",
                    " "
            ));
            book.setItemMeta(bookMeta);
        }
        inv.setItem(15, book);

        // Always opens — no longer gated inside if(sm!=null)
        player.openInventory(inv);
    }
}