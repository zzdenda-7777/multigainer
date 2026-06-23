package multigainer.multigainer.levels;

import multigainer.multigainer.math.BigNumber;

public class MiningLevelManager {

    /**
     * Calculates the exact XP required to advance from the current mining level to the next.
     * Uses your custom baseline of 5 scaling exponentially by 1.00025 per tier.
     */
    public static double getRequiredXpForNextLevel(int currentLevel) {
        if (currentLevel < 1) return 5.0;
        return 5.0 + Math.pow(5.0 * currentLevel, 1.00025);
    }

    /**
     * Calculates the exponential money multiplier based on mining level (1.1x compounded per level).
     * Level 1 = 1.1x, Level 2 = 1.21x, Level 3 = 1.331x...
     */
    public static BigNumber getMoneyMultiplier(int currentLevel) {
        return new BigNumber(Math.pow(1.1, currentLevel));
    }

    /**
     * Calculates the exponential gems multiplier based on mining level (1.02x compounded per level).
     */
    public static BigNumber getGemsMultiplier(int currentLevel) {
        return new BigNumber(Math.pow(1.02, currentLevel));
    }

    /**
     * Generates a sleek visual progress bar for the mining scoreboard interface.
     */
    public static String generateXpBar(double currentXp, double requiredXp) {
        int totalBars = 10;
        double progress = Math.min(1.0, Math.max(0.0, currentXp / requiredXp));
        int filledCount = (int) (progress * totalBars);
        int emptyCount = totalBars - filledCount;

        StringBuilder progressBar = new StringBuilder("§8[");

        // Aqua/Cyan filled progress segments for mining themes
        progressBar.append("§b");
        for (int i = 0; i < filledCount; i++) {
            progressBar.append("■");
        }

        // Dark gray empty remaining segments
        progressBar.append("§7");
        for (int i = 0; i < emptyCount; i++) {
            progressBar.append("■");
        }

        progressBar.append("§8] §e").append((int) (progress * 100)).append("%");
        return progressBar.toString();
    }
}
