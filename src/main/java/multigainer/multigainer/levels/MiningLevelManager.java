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
     * Uses a gradient from white to dark gray for filled segments.
     */
    public static String generateXpBar(double currentXp, double requiredXp) {
        int totalBars = 10;
        double progress = Math.min(1.0, Math.max(0.0, currentXp / requiredXp));
        int filledCount = (int) (progress * totalBars);
        int emptyCount = totalBars - filledCount;

        // Gradient colors from white to dark gray (10 hex colors)
        String[] gradientColors = {
            "§x§F§F§F§F§F§F", // #FFFFFF (white)
            "§x§E§C§E§C§E§C", // #ECECEC
            "§x§D§9§D§9§D§9", // #D9D9D9
            "§x§C§6§C§6§C§6", // #C6C6C6
            "§x§B§3§B§3§B§3", // #B3B3B3
            "§x§9§9§9§9§9§9", // #999999
            "§x§8§0§8§0§8§0", // #808080
            "§x§6§6§6§6§6§6", // #666666
            "§x§4§D§4§D§4§D", // #4D4D4D
            "§x§3§3§3§3§3§3"  // #333333 (darkest)
        };

        StringBuilder progressBar = new StringBuilder("§8[");

        // Appends the gradient filled progress segments
        for (int i = 0; i < filledCount; i++) {
            progressBar.append(gradientColors[i]);
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
