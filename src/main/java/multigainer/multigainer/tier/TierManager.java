package multigainer.multigainer.tier;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit; // Imported to handle global server broadcasts
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TierManager {

    // BigNumber version — avoids double overflow (Infinity) at tier 17+
    // log10(cost) = 5 * 1.3^(nextTier-1)
    public static BigNumber getCostForTierBig(int nextTier) {
        if (nextTier <= 1) return new BigNumber(100000.0);
        double logCost = 5.0 * Math.pow(1.3, nextTier - 1);
        if (!Double.isFinite(logCost)) return new BigNumber(1.0, 1.0e15);
        double exponent = Math.floor(logCost);
        double mantissa = Math.pow(10, logCost - exponent);
        return new BigNumber(mantissa, exponent);
    }

    // Legacy double version — capped to avoid Infinity
    public static double getCostForTier(int nextTier) {
        if (nextTier <= 1) return 100000.0;
        double result = Math.pow(100000.0, Math.pow(1.3, nextTier - 1));
        return Double.isFinite(result) ? result : Double.MAX_VALUE;
    }

    /**
     * Determines global multi rewards. Tier 0 starts at standard 1x.
     */
    public static double getMultiplierForTier(int tier) {
        return Math.pow(2, tier);
    }

    public static void performTierUp(PlayerProfile profile, Player player) {
        // CHANGED: Instead of subtracting the cost, your rebirth points are completely reset to 0
        profile.setRebirthPoints(0.0);

        int newTier = profile.getTier() + 1;
        profile.setTier(newTier);

        // Calculate and add Tier Points sequentially (+1 for Tier 1, +2 for Tier 2, etc.)
        profile.setTierPoints(profile.getTierPoints() + newTier);

        // Clear progress vectors back to base level settings
        profile.setMoney(new BigNumber(0));
        profile.setUpgradeLevel(0);
        profile.setGemUpgradeLevel(0);
        profile.setFarmMultiUpgradeLevel(0);

        // Display Screen Elements (Title, Subtitle, Fade In, Stay, Fade Out Ticks)
        player.sendTitle(
                "§a§lTIER UP",
                "§7You have tiered up to Tier §e" + newTier,
                10, 50, 10
        );

        // ADDED: Global chat announcement style layout
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§b§l⚡ TIER ADVANCEMENT ⚡");
        Bukkit.broadcastMessage("§f" + player.getName() + " §7has successfully ascended to §a§lTIER " + newTier + "§7!");
        Bukkit.broadcastMessage("§7They received §d+" + newTier + " Tier Points§7!");
        Bukkit.broadcastMessage(" ");
    }
}