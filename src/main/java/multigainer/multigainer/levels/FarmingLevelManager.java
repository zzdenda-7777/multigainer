package multigainer.multigainer.levels;

public class FarmingLevelManager {

    /**
     * Calculates the exact XP required to advance from the current level to the next.
     * Uses your custom baseline of 25 scaling exponentially by 1.001 per tier.
     */
    public static double getRequiredXpForNextLevel(int currentLevel) {
        if (currentLevel < 1) return 25.0;
        return 25.0 + Math.pow(25.0 * currentLevel, 1.001);
    }

    /**
     * Generates a sleek, modern visual progress bar for the scoreboard interface.
     * Example: [■■■■■■■■□□] 80%
     * Uses a gradient from light yellow to dark gold for filled segments.
     */
    public static String generateXpBar(double currentXp, double requiredXp) {
        int totalBars = 10;
        double progress = Math.min(1.0, Math.max(0.0, currentXp / requiredXp));
        int filledCount = (int) (progress * totalBars);
        int emptyCount = totalBars - filledCount;

        // Gradient colors from light yellow to dark gold (10 hex colors)
        String[] gradientColors = {
            "§x§F§F§F§9§C§4", // #FFF9C4 (lightest)
            "§x§F§F§F§5§9§D", // #FFF59D
            "§x§F§F§E§E§5§8", // #FFEE58
            "§x§F§D§D§8§3§5", // #FDD835
            "§x§F§B§C§0§2§D", // #FBC02D
            "§x§F§9§A§8§2§5", // #F9A825
            "§x§F§5§7§F§1§7", // #F57F17
            "§x§E§6§5§1§0§0", // #E65100
            "§x§D§8§4§3§1§5", // #D84315
            "§x§B§F§3§6§0§C"  // #BF360C (darkest)
        };

        StringBuilder progressBar = new StringBuilder("§8[");

        // Appends the gradient filled progress segments
        for (int i = 0; i < filledCount; i++) {
            progressBar.append(gradientColors[i]);
            progressBar.append("■");
        }

        // Appends the dark gray empty remaining segments
        progressBar.append("§7");
        for (int i = 0; i < emptyCount; i++) {
            progressBar.append("■");
        }

        progressBar.append("§8] §e").append((int) (progress * 100)).append("%");
        return progressBar.toString();
    }
}
