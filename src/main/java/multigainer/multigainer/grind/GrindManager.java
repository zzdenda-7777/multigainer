package multigainer.multigainer.grind;

public class GrindManager {

    // ── Cost formulas: cost(nextLevel) = base * nextLevel ^ power ─────────────

    public static double getChanceCost(int nextLevel)    { return 5.0  * Math.pow(nextLevel, 1.01);   }
    public static double getExponentCost(int nextLevel)  { return 10.0 * Math.pow(nextLevel, 1.015);  }
    public static double getFarmMultiCost(int nextLevel) { return 3.0  * Math.pow(nextLevel, 1.0025); }
    public static double getGemMultiCost(int nextLevel)  { return 7.5  * Math.pow(nextLevel, 1.01);   }
    public static double getFarmXpCost(int nextLevel)    { return 5.0  * Math.pow(nextLevel, 1.075);  }
    public static double getMineXpCost(int nextLevel)    { return 10.0 * Math.pow(nextLevel, 1.01);   }
    public static double getSeedMultiCost(int nextLevel) { return 10.0 * Math.pow(nextLevel, 1.02);   }
    public static double getGPMultiCost(int nextLevel)   { return 25.0 * Math.pow(nextLevel, 1.05);   }

    // ── Drop chance denominators ───────────────────────────────────────────────
    // Farming: 1/500 base, Mining: 1/100 base.
    // Each chance upgrade reduces both denominators by 1% compound.

    public static double getFarmingChanceDenominator(int chanceLevel) {
        return 500.0 * Math.pow(0.99, chanceLevel);
    }

    public static double getMiningChanceDenominator(int chanceLevel) {
        return 100.0 * Math.pow(0.99, chanceLevel);
    }

    // Returns chance as percentage (e.g. 1.0 = 1%)
    public static double getFarmingChancePct(int chanceLevel) {
        return 100.0 / getFarmingChanceDenominator(chanceLevel);
    }

    public static double getMiningChancePct(int chanceLevel) {
        return 100.0 / getMiningChanceDenominator(chanceLevel);
    }

    // ── Effect calculations ────────────────────────────────────────────────────

    // Money exponent: default 1.0, +0.01 per level
    public static double getMoneyExponent(int level) {
        return 1.0 + 0.01 * level;
    }

    // Farm multi applied to money income: 1.15^level
    public static double getFarmMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.15, level);
    }

    // Gem multi applied to gem income: 1.05^level
    public static double getGemMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.05, level);
    }

    // Farm XP multiplier: 1.075^level
    public static double getFarmXpMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.075, level);
    }

    // Mine XP multiplier: 1.04^level
    public static double getMineXpMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.04, level);
    }

    // Seed multiplier: 1.03^level
    public static double getSeedMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.03, level);
    }

    // GP multiplier: 1.25^level
    public static double getGPMulti(int level) {
        return level <= 0 ? 1.0 : Math.pow(1.25, level);
    }
}
