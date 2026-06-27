package multigainer.multigainer.tier;

import multigainer.multigainer.armor.ArmorManager;
import multigainer.multigainer.armor.ArmorType;
import multigainer.multigainer.artifacts.ArtifactManager;
import multigainer.multigainer.artifacts.ArtifactType;
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

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        java.util.UUID uid    = player.getUniqueId();
        int tier              = profile.getTier();
        BigNumber tierMulti   = TierManager.getMultiplierForTier(tier);
        BigNumber nextCost    = TierManager.getCostForTierBig(tier + 1);
        BigNumber curPoints   = profile.getRebirthPoints();
        boolean canTier       = curPoints.compareTo(nextCost) >= 0;

        String tierMultiStr = NumberFormatter.format(tierMulti, uid) + "x";

        // ── Slot 11: Player head with stats ──────────────────────────────────
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm    = (SkullMeta) stats.getItemMeta();
        if (sm != null) {
            sm.setOwningPlayer(player);
            sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "TIER " + tier);
            sm.setLore(Arrays.asList(
                    " ",
                    ChatColor.GRAY + "Current Tier§8: §e" + ChatColor.BOLD + tier,
                    ChatColor.GRAY + "Current Multi§8: §6" + tierMultiStr,
                    ChatColor.GRAY + "Tier Points§8: §d" + NumberFormatter.format(new BigNumber(profile.getTierPoints()), uid),
                    " ",
                    ChatColor.GRAY + "Next Tier Cost§8: §b"
                            + NumberFormatter.format(nextCost, uid) + " §7Points",
                    ChatColor.GRAY + "Your Points§8: §b"
                            + NumberFormatter.format(curPoints, uid)
            ));
            stats.setItemMeta(sm);
        }
        inv.setItem(11, stats);

        // ── Tier points calculation for slot 13 hover ────────────────────────
        int    newTierIfUp      = tier + 1;
        double artifactTierMult = ArtifactManager.getMultiplierDouble(profile, ArtifactType.TIER_POINTS);
        double armorTierMult    = ArmorManager.getMultiplier(profile, ArmorType.TIER_POINTS);
        double exactTierPtsIfUp = newTierIfUp * artifactTierMult * armorTierMult;
        int    tierPtsWouldGet  = Math.max(newTierIfUp, (int) Math.round(exactTierPtsIfUp));
        double totalTierMulti   = artifactTierMult * armorTierMult;
        String totalMultiStr    = NumberFormatter.format(new BigNumber(totalTierMulti), uid) + "x";

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
                    ChatColor.GRAY + "Progress§8: §b"
                            + NumberFormatter.format(curPoints, uid)
                            + ChatColor.GRAY + "§8/§6"
                            + NumberFormatter.format(nextCost, uid),
                    " ",
                    ChatColor.GRAY + "Tier Points Reward§8: §e+"
                            + NumberFormatter.format(new BigNumber(tierPtsWouldGet), uid),
                    ChatColor.GRAY + "Total Multi§8: §6" + totalMultiStr,
                    " ",
                    canTier ? ChatColor.YELLOW + "✔ Click to TIER UP!" : ChatColor.RED + "✘ Not enough points."
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

        player.openInventory(inv);
    }
}
