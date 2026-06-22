package multigainer.multigainer.upgrades;

import multigainer.multigainer.math.BigNumber;

public class UpgradeManager {
    public static final int MAX_LEVEL = 27;

    public static BigNumber getUpgradeCost(int level) {
        if (level == 1) return new BigNumber(500.0);
        if (level == 2) return new BigNumber(2500.0);

        // Exponential scale targeting an e10,000 cost at Level 27
        double startExp = Math.log10(2500.0);
        double targetExp = 10000.0;
        double currentExp = startExp + ((double) (level - 2) * (targetExp - startExp) / (MAX_LEVEL - 2));

        double mantissa = Math.pow(10, currentExp % 1);
        double exponent = Math.floor(currentExp);
        return new BigNumber(mantissa, exponent);
    }

    public static double getTierMultiplier(int level) {
        if (level == 1) return 2.0;
        if (level == 2) return 3.0;
        return 3.0 + ((level - 2) * 0.28);
    }

    public static BigNumber getTotalMultiplier(int currentOwnedLevel) {
        double product = 1.0;
        for (int i = 1; i <= currentOwnedLevel; i++) {
            product *= getTierMultiplier(i);
        }
        return new BigNumber(product);
    }
}
