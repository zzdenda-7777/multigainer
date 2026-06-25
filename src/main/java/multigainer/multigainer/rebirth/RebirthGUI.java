package multigainer.multigainer.rebirth;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber; // Added import for BigNumber conversion
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

public class RebirthGUI {
    public static void open(Player player, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Rebirth");

        // Decorative Fill
        // 1. Statistics Section (Slot 11)
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) stats.getItemMeta(); // Změněno na SkullMeta a přetypováno

        if (sm != null) {
            // Nastavení hlavy hráče
            sm.setOwningPlayer(player);

            // Nastavení jména a lore
            sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "YOUR PROGRESS");
            sm.setLore(Arrays.asList(
                    " ",
                    ChatColor.GRAY + "Money" + ChatColor.GREEN + NumberFormatter.format(profile.getMoney()),
                    ChatColor.GRAY + "Rebirth Points: " + ChatColor.AQUA + NumberFormatter.format(new BigNumber(profile.getRebirthPoints())),
                    ChatColor.GRAY + "Active Multiplier: " + ChatColor.YELLOW + NumberFormatter.format(new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()))) + "x",
                    " "
            ));

            stats.setItemMeta(sm); // Uložení metadat zpět do itemu
        }

// Vložení do slotu 11
        inv.setItem(11, stats);

        // 2. Rebirth Action (Slot 13)
        double potential = RebirthManager.calculateRebirthPoints(profile.getMoney().toDouble());
        boolean canRebirth = profile.getMoney().toDouble() >= RebirthManager.REBIRTH_THRESHOLD;

        ItemStack rebirth = new ItemStack(canRebirth ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta rm = rebirth.getItemMeta();
        rm.setDisplayName(canRebirth ? ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "REBIRTH" : ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
        rm.setLore(Arrays.asList(
                // FIXED: Automatically format the static cost threshold using BigNumber definitions
                " ",
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + NumberFormatter.format(new BigNumber(RebirthManager.REBIRTH_THRESHOLD)) + " Money",
                " ",
                ChatColor.GRAY + "Rebirth reset your progress,",
                ChatColor.GRAY + "but you will be granted:",
                // FIXED: Formatted potential point yield to fix the issue shown in image_346d49.png
                ChatColor.AQUA + "+ " + NumberFormatter.format(new BigNumber(potential)) + " Rebirth Points"
        ));
        rebirth.setItemMeta(rm);

        // 3. Informational Guide (Slot 15)
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = info.getItemMeta();

        im.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "PROGRESS GUIDE");
        im.setLore(Arrays.asList(
                " ",
                ChatColor.WHITE + "REBIRTH PATH",
                ChatColor.GRAY + "Accumulate " + ChatColor.YELLOW + NumberFormatter.format(new BigNumber(RebirthManager.REBIRTH_THRESHOLD)) + " money",
                ChatColor.GRAY + "to qualify for a rebirth.",
                " ",
                ChatColor.WHITE + "THE BENEFITS",
                ChatColor.GRAY + "Reset your current stats to earn",
                ChatColor.GRAY + "permanent multipliers that boost",
                ChatColor.GRAY + "your future speed significantly.",
                " ",
                ChatColor.WHITE + "CALCULATION",
                ChatColor.GRAY + "Rebirth points are derived from",
                ChatColor.GRAY + "the cube root of your total money.",
                ChatColor.GRAY + "Multiplier scales with your points."
        ));
        info.setItemMeta(im);

        inv.setItem(11, stats);
        inv.setItem(13, rebirth);
        inv.setItem(15, info);
        player.openInventory(inv);
    }
}
