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
     */
    public static String generateXpBar(double currentXp, double requiredXp) {
        int totalBars = 10;
        double progress = Math.min(1.0, Math.max(0.0, currentXp / requiredXp));
        int filledCount = (int) (progress * totalBars);
        int emptyCount = totalBars - filledCount;

        StringBuilder progressBar = new StringBuilder("§8[");

        // Appends the green filled progress segments
        progressBar.append("§a");
        for (int i = 0; i < filledCount; i++) {
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
