package multigainer.multigainer.perks;

import multigainer.multigainer.upgrades.UpgradeManager;
import multigainer.multigainer.math.BigNumber;

public class PerkManager {

    public static final int PERK_COUNT = 5;

    public static final String[] PERK_NAMES = {
        "Mineral Sense", "Ore Pulse", "Crystal Touch", "Gem Sight", "Void Resonance"
    };

    public static final String[] PERK_COLORS = {
        "§a", "§e", "§6", "§c", "§d"
    };

    // Base multiplier applied per find (exponential)
    public static final double[] PERK_BASE_MULTIPLIERS = {1.01, 1.02, 1.03, 1.05, 1.1};

    // Default drop chance denominator (1/X)
    public static final double[] PERK_BASE_CHANCES = {100.0, 250.0, 625.0, 2000.0, 7500.0};

    // Drop chance denominator after upgradeLevel upgrades (-2% compound each)
    public static double getPerkChanceDenominator(int perkIndex, int upgradeLevel) {
        return PERK_BASE_CHANCES[perkIndex] * Math.pow(0.98, upgradeLevel);
    }

    // Perk upgrade cost: 3 GP at level 1, ×nextLevel^1.02 each step
    public static double getPerkUpgradeCost(int nextLevel) {
        if (nextLevel <= 0) return 3.0;
        return 3.0 * Math.pow(nextLevel, 1.02);
    }

    // Total perk multiplier as BigNumber (safe for huge counts via log10 math)
    public static BigNumber getTotalPerkMultiplierBig(int[] counts) {
        double log10 = 0;
        for (int i = 0; i < PERK_COUNT; i++) {
            if (counts[i] > 0) {
                log10 += counts[i] * Math.log10(PERK_BASE_MULTIPLIERS[i]);
            }
        }
        return UpgradeManager.fromLog10(log10);
    }

    // Individual perk multiplier as BigNumber
    public static BigNumber getPerkMultiplierBig(int perkIndex, int count) {
        if (count <= 0) return new BigNumber(1.0);
        double log10 = count * Math.log10(PERK_BASE_MULTIPLIERS[perkIndex]);
        return UpgradeManager.fromLog10(log10);
    }
}
