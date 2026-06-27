package multigainer.multigainer.rebirth;

import multigainer.multigainer.artifacts.ArtifactManager;
import multigainer.multigainer.artifacts.ArtifactType;
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
        java.util.UUID uid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, "§8Rebirth");

        // Decorative Fill
        // 1. Statistics Section (Slot 11)
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) stats.getItemMeta();

        if (sm != null) {
            sm.setOwningPlayer(player);
            sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "YOUR PROGRESS");
            sm.setLore(Arrays.asList(
                    " ",
                    ChatColor.GRAY + "Money: " + ChatColor.GREEN + NumberFormatter.format(profile.getMoney(), uid),
                    ChatColor.GRAY + "Rebirth Points: " + ChatColor.AQUA + NumberFormatter.format(profile.getRebirthPoints(), uid),
                    ChatColor.GRAY + "Active Multiplier: " + ChatColor.YELLOW + NumberFormatter.format(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()), uid) + "x",
                    " "
            ));

            stats.setItemMeta(sm); // Uložení metadat zpět do itemu
        }

// Vložení do slotu 11
        inv.setItem(11, stats);

        // 2. Rebirth Action (Slot 13)
        BigNumber potential = RebirthManager.calculateRebirthPoints(profile.getMoney())
                .multiply(new BigNumber(ArtifactManager.getMultiplierDouble(profile, ArtifactType.REBIRTH_POINTS)));
        boolean canRebirth = profile.getMoney().compareTo(new BigNumber(RebirthManager.REBIRTH_THRESHOLD)) >= 0;

        ItemStack rebirth = new ItemStack(canRebirth ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta rm = rebirth.getItemMeta();
        rm.setDisplayName(canRebirth ? ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "REBIRTH" : ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
        rm.setLore(Arrays.asList(
                " ",
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + NumberFormatter.format(new BigNumber(RebirthManager.REBIRTH_THRESHOLD), uid) + " Money",
                " ",
                ChatColor.GRAY + "Rebirth reset your progress,",
                ChatColor.GRAY + "but you will be granted:",
                ChatColor.AQUA + "+ " + NumberFormatter.format(potential, uid) + " Rebirth Points"
        ));
        rebirth.setItemMeta(rm);

        // 3. Informational Guide (Slot 15)
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = info.getItemMeta();

        im.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "PROGRESS GUIDE");
        im.setLore(Arrays.asList(
                " ",
                ChatColor.WHITE + "REBIRTH PATH",
                ChatColor.GRAY + "Accumulate " + ChatColor.YELLOW + NumberFormatter.format(new BigNumber(RebirthManager.REBIRTH_THRESHOLD), uid) + " money",
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