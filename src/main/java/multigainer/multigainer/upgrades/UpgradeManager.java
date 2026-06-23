package multigainer.multigainer.upgrades;

import multigainer.multigainer.math.BigNumber;

public class UpgradeManager {
    public static final int MAX_LEVEL = 27;

    // 27 completely separate costs explicitly written down scaling up to e10000
    public static BigNumber getUpgradeCost(int level) {
        switch (level) {
            case 1: return new BigNumber(500.0);
            case 2: return new BigNumber(2500.0);
            case 3: return new BigNumber(50000.0);     // 50k
            case 4: return new BigNumber(1000000.0);   // 1m
            case 5: return new BigNumber(50000000.0);  // 50m
            case 6: return new BigNumber(5.0, 9.0);    // 5b
            case 7: return new BigNumber(5.0, 13.0);   // 50t
            case 8: return new BigNumber(1.0, 15.0);   // 1qn
            case 9: return new BigNumber(7.5, 18.0);   // 7.5Qi
            case 10: return new BigNumber(2.0, 24.0);  // Septillion scales
            case 11: return new BigNumber(4.2, 36.0);
            case 12: return new BigNumber(1.0, 50.0);
            case 13: return new BigNumber(8.8, 75.0);
            case 14: return new BigNumber(3.0, 110.0);
            case 15: return new BigNumber(1.5, 160.0);
            case 16: return new BigNumber(9.0, 240.0);
            case 17: return new BigNumber(5.0, 380.0);
            case 18: return new BigNumber(2.2, 550.0);
            case 19: return new BigNumber(7.7, 800.0);
            case 20: return new BigNumber(1.0, 1200.0);
            case 21: return new BigNumber(4.0, 1800.0);
            case 22: return new BigNumber(3.3, 2700.0);
            case 23: return new BigNumber(6.5, 4000.0);
            case 24: return new BigNumber(1.2, 5800.0);
            case 25: return new BigNumber(8.0, 7500.0);
            case 26: return new BigNumber(4.5, 9000.0);
            case 27: return new BigNumber(1.0, 10000.0); // e10000 Max target
            default: return new BigNumber(1.0, 10000.0);
        }
    }

    // 27 completely separate tier multipliers (each between 2x and 10x)
    public static double getTierMultiplier(int level) {
        switch (level) {
            case 1: return 2.0;
            case 2: return 3.0;
            case 3: return 2.0;
            case 4: return 4.0;
            case 5: return 5.0;
            case 6: return 3.0;
            case 7: return 6.0;
            case 8: return 4.0;
            case 9: return 7.0;
            case 10: return 5.0;
            case 11: return 8.0;
            case 12: return 4.0;
            case 13: return 9.0;
            case 14: return 6.0;
            case 15: return 10.0;
            case 16: return 5.0;
            case 17: return 8.0;
            case 18: return 6.0;
            case 19: return 9.0;
            case 20: return 7.0;
            case 21: return 10.0;
            case 22: return 5.0;
            case 23: return 8.0;
            case 24: return 6.0;
            case 25: return 9.0;
            case 26: return 7.0;
            case 27: return 10.0;
            default: return 1.0;
        }
    }

    // Compounds the multipliers multiplicatively (e.g., 2x * 3x * 2x = 12x)
    public static BigNumber getTotalMultiplier(int currentOwnedLevel) {
        BigNumber total = new BigNumber(1.0);
        for (int i = 1; i <= currentOwnedLevel; i++) {
            total = total.multiply(new BigNumber(getTierMultiplier(i)));
        }
        return total;
    }
}