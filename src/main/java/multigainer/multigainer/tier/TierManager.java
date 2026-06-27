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

        // Calculate and add Tier Points (multiplied by tier points artifact)
        double artifactTierMult = ArtifactManager.getMultiplierDouble(profile, ArtifactType.TIER_POINTS);
        double armorTierMult    = ArmorManager.getMultiplier(profile, ArmorType.TIER_POINTS);
        double exactTierPts     = newTier * artifactTierMult * armorTierMult;
        int tierPtsToAdd        = Math.max(newTier, (int) Math.round(exactTierPts));
        profile.setTierPoints(profile.getTierPoints() + tierPtsToAdd);
        String tierPtsDisplay   = (exactTierPts == Math.floor(exactTierPts))
                ? String.valueOf((long) exactTierPts)
                : String.format("%.2f", exactTierPts).replaceAll("0+$", "").replaceAll("\\.$", "");

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

        // Global chat announcement
        double totalTierMulti = artifactTierMult * armorTierMult;
        String totalMultiStr  = NumberFormatter.format(new BigNumber(totalTierMulti));
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§b§l⚡ TIER ADVANCEMENT ⚡");
        Bukkit.broadcastMessage("§f" + player.getName() + " §7has successfully ascended to §a§lTIER " + newTier + "§7!");
        Bukkit.broadcastMessage("§7They received §e+" + tierPtsDisplay + " Tier Points §8(§6" + totalMultiStr + "x §7multi§8)§7!");
        Bukkit.broadcastMessage(" ");
    }
}