package multigainer.multigainer.rebirth;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RebirthGUI {
    public static void open(Player player, PlayerProfile profile) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Rebirth Terminal");

        // Decorative Fill
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // 1. Statistics Section (Slot 11)
        ItemStack stats = new ItemStack(Material.PLAYER_HEAD); // Using Head as a stat icon
        ItemMeta sm = stats.getItemMeta();
        sm.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "YOUR PROGRESS");
        sm.setLore(Arrays.asList(
                " ",
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Current Funds: " + ChatColor.GREEN + NumberFormatter.format(profile.getMoney()),
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Rebirth Points: " + ChatColor.AQUA + String.format("%.2f", profile.getRebirthPoints()),
                ChatColor.GRAY + "● " + ChatColor.WHITE + "Active Multiplier: " + ChatColor.YELLOW + String.format("%.2fx", RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints())),
                " "
        ));
        stats.setItemMeta(sm);

        // 2. Rebirth Action (Slot 13)
        double potential = RebirthManager.calculateRebirthPoints(profile.getMoney().toDouble());
        boolean canRebirth = profile.getMoney().toDouble() >= RebirthManager.REBIRTH_THRESHOLD;

        ItemStack rebirth = new ItemStack(canRebirth ? Material.NETHER_STAR : Material.BARRIER);
        ItemMeta rm = rebirth.getItemMeta();
        rm.setDisplayName(canRebirth ? ChatColor.GREEN + "" + ChatColor.BOLD + "INITIATE REBIRTH" : ChatColor.RED + "" + ChatColor.BOLD + "LOCKED");
        rm.setLore(Arrays.asList(
                ChatColor.GRAY + "Cost: " + ChatColor.WHITE + "500,000 Money",
                " ",
                ChatColor.GRAY + "Upon rebirth, your stats reset",
                ChatColor.GRAY + "but you will be granted:",
                ChatColor.AQUA + "➜ " + String.format("%.2f", potential) + " Rebirth Points"
        ));
        rebirth.setItemMeta(rm);

        // 3. Informational Guide (Slot 15)
        ItemStack info = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(ChatColor.BLUE + "" + ChatColor.BOLD + "REBIRTH ACADEMY");
        im.setLore(Arrays.asList(
                ChatColor.DARK_GRAY + "Master your progression:",
                " ",
                ChatColor.GRAY + "1. Reach the goal of 500k money.",
                ChatColor.GRAY + "2. Rebirth to reset your progress.",
                ChatColor.GRAY + "3. Earn permanent multipliers",
                ChatColor.GRAY + "   to accelerate your future growth.",
                " ",
                ChatColor.BLUE + "» Point Formula: " + ChatColor.WHITE + "∛(Money)",
                ChatColor.BLUE + "» Multiplier: " + ChatColor.WHITE + "Points^(1/15)"
        ));
        info.setItemMeta(im);

        inv.setItem(11, stats);
        inv.setItem(13, rebirth);
        inv.setItem(15, info);
        player.openInventory(inv);
    }
}