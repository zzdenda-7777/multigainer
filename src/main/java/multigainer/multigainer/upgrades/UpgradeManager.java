package multigainer.multigainer.upgrades;

import multigainer.multigainer.math.BigNumber;

public class UpgradeManager {
    public static final int MONEY_MAX_LEVEL = 100;
    public static final int GEM_MAX_LEVEL   = 100;
    // Backward compatibility aliases
    public static final int MAX_LEVEL = MONEY_MAX_LEVEL;
    public static BigNumber getUpgradeCost(int level)     { return getMoneyUpgradeCost(level); }
    public static BigNumber getTotalMultiplier(int level)  { return getMoneyTotalMultiplier(level); }
    public static double    getTierMultiplier(int level)   { return getMoneyTierMultiplier(level); }

    // Money: cost(n) = 500^(1.3^(n-1))  [was 1.5, reduced for easier progression]
    public static BigNumber getMoneyUpgradeCost(int level) {
        double logCost = Math.pow(1.3, level - 1) * Math.log10(500.0);
        return fromLog10(logCost);
    }

    // Per-level money multiplier cubic polynomial
    public static double getMoneyTierMultiplier(int level) {
        double n = level;
        double logMulti = 3.261e-5 * n*n*n - 2.252e-3 * n*n + 0.1019 * n + 0.2013;
        return Math.pow(10, logMulti);
    }

    // Total money multiplier via closed-form sums (no loop)
    public static BigNumber getMoneyTotalMultiplier(int level) {
        if (level <= 0) return new BigNumber(1.0);
        double n = level;
        double sumCubes   = n * n * (n + 1) * (n + 1) / 4.0;
        double sumSquares = n * (n + 1) * (2 * n + 1) / 6.0;
        double sumLinear  = n * (n + 1) / 2.0;
        double totalLog   = 3.261e-5 * sumCubes - 2.252e-3 * sumSquares + 0.1019 * sumLinear + 0.2013 * n;
        return fromLog10(totalLog);
    }

    // Gem: cost(n) = 2500^(1.2^(n-1)), total = 1.25^level  [was 1.4, reduced]
    public static BigNumber getGemUpgradeCost(int level) {
        double logCost = Math.pow(1.2, level - 1) * Math.log10(2500.0);
        return fromLog10(logCost);
    }
    public static BigNumber getGemTotalMultiplier(int level) {
        if (level <= 0) return new BigNumber(1.0);
        return fromLog10(level * Math.log10(1.25));
    }

    // Farm: cost(n) = 2500^(1.1^(n-1)), total = 2^level  [was 1.3, reduced]
    public static BigNumber getFarmUpgradeCost(int level) {
        double logCost = Math.pow(1.1, level - 1) * Math.log10(2500.0);
        return fromLog10(logCost);
    }
    public static BigNumber getFarmTotalMultiplier(int level) {
        if (level <= 0) return new BigNumber(1.0);
        return fromLog10(level * Math.log10(2.0));
    }
    public static double getFarmTotalMultiplierDouble(int level) {
        if (level <= 0) return 1.0;
        double result = Math.pow(2.0, level);
        return Double.isFinite(result) ? result : Double.MAX_VALUE;
    }

    public static BigNumber fromLog10(double logValue) {
        if (!Double.isFinite(logValue)) return new BigNumber(1.0, 1.0e15);
        double exponent = Math.floor(logValue);
        double mantissa = Math.pow(10, logValue - exponent);
        return new BigNumber(mantissa, exponent);
    }
}