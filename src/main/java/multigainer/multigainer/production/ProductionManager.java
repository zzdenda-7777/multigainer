package multigainer.multigainer.production;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.PickaxeManager;
import org.bukkit.entity.Player;

public class ProductionManager {

    // XP awarded per block type sent to production (index matches PickaxeManager.BLOCKS)
    public static final int[] BLOCK_XP_VALUES = {
        1, 2, 3, 4, 5, 7, 9, 12, 16, 22, 29, 38, 50, 63, 77, 90, 100
    };

    // XP needed to advance from currentLevel to currentLevel+1
    // Formula: 100 * (level)^1.01  — level 1=100, level 2≈201, level 3≈304
    public static double getXpForNextLevel(int currentLevel) {
        return 100.0 * Math.pow(currentLevel + 1, 1.01);
    }

    // Energy generated per minute: 0.05 base, each level's increment ×1.02
    // Sum of geometric series: 0.05 × (1.02^level - 1) / 0.02
    public static double getEnergyPerMinute(int level) {
        if (level <= 0) return 0.0;
        return 0.05 * (Math.pow(1.02, level) - 1.0) / 0.02;
    }

    public static void addWorkerXp(PlayerProfile profile, double xpToAdd) {
        profile.setWorkerXp(profile.getWorkerXp() + xpToAdd);
        double needed;
        while (profile.getWorkerXp() >= (needed = getXpForNextLevel(profile.getWorkerLevel()))) {
            profile.setWorkerXp(profile.getWorkerXp() - needed);
            profile.setWorkerLevel(profile.getWorkerLevel() + 1);
        }
    }

    public static void sendBlocksToProduction(Player player, PlayerProfile profile, int blockIndex, long amount) {
        long stored = profile.getBlockStorage(blockIndex);
        if (stored <= 0) {
            player.sendMessage("§cNo " + PickaxeManager.BLOCK_NAMES[blockIndex] + " stored!");
            return;
        }
        long toSend   = Math.min(amount, stored);
        double xpEach = BLOCK_XP_VALUES[blockIndex];
        double totalXp = toSend * xpEach;
        profile.addBlockStorage(blockIndex, -toSend);
        addWorkerXp(profile, totalXp);
        player.sendMessage("§7Sent §f" + toSend + " §7" + PickaxeManager.BLOCK_NAMES[blockIndex]
            + " §8→ §a+" + NumberFormatter.format(new BigNumber(totalXp)) + " §7Work XP");
    }
}
