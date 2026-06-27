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

    // cost(n) = cost(n-1)^1.2  →  log10(cost(n)) = 5 * 1.2^(n-1)
    // Stage 1: compute log10(logCost) = log10(5) + (n-1)*log10(1.2)
    //          This intermediate value NEVER overflows for any int tier.
    // Stage 2: raise 10 to that power to get logCost.
    //          Only overflows (~tier 3886+) when the cost exceeds BigNumber's max (10^(1.79e308)).
    //          In that case we return the true BigNumber ceiling — no artificial cap.
    public static BigNumber getCostForTierBig(int nextTier) {
        if (nextTier <= 1) return new BigNumber(100000.0);
        double logOfLogCost = Math.log10(5.0) + (nextTier - 1) * Math.log10(1.2);
        double logCost      = Math.pow(10.0, logOfLogCost);
        if (!Double.isFinite(logCost)) {
            // The cost exceeds what BigNumber can store (its exponent is a double).
            // Return the maximum representable BigNumber: ~10^(1e308).
            return new BigNumber(1.0, 1.0e308);
        }
        double exponent = Math.floor(logCost);
        double mantissa = Math.pow(10.0, logCost - exponent);
        return new BigNumber(mantissa, exponent);
    }

    // 2^tier as BigNumber via log10 — never overflows regardless of tier
    public static BigNumber getMultiplierForTier(int tier) {
        if (tier <= 0) return new BigNumber(1.0);
        double logValue = tier * Math.log10(2.0);
        double exponent = Math.floor(logValue);
        double mantissa = Math.pow(10, logValue - exponent);
        return new BigNumber(mantissa, exponent);
    }

    public static void performTierUp(PlayerProfile profile, Player player) {
        // CHANGED: Instead of subtracting the cost, your rebirth points are completely reset to 0
        profile.setRebirthPoints(new BigNumber(0));

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
        String totalMultiStr  = NumberFormatter.format(new BigNumber(totalTierMulti), player.getUniqueId());
        Bukkit.broadcastMessage(" ");
        Bukkit.broadcastMessage("§b§l⚡ TIER ADVANCEMENT ⚡");
        Bukkit.broadcastMessage("§f" + player.getName() + " §7has successfully ascended to §a§lTIER " + newTier + "§7!");
        Bukkit.broadcastMessage("§7They received §e+" + tierPtsDisplay + " Tier Points §8(§6" + totalMultiStr + "x §7multi§8)§7!");
        Bukkit.broadcastMessage(" ");
    }
}