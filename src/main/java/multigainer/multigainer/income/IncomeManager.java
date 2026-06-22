package multigainer.multigainer.income;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber; // Added missing import
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class IncomeManager {
    private final Multigainer plugin;

    public IncomeManager(Multigainer plugin) {
        this.plugin = plugin;
        startPassiveIncomeTask();
    }

    private void startPassiveIncomeTask() {
        // Runs on a repeating schedule every 20 ticks (exactly 1 second)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

                    // Default configuration base values
                    double baseIncome = 1.0;

                    // Placeholder for future logic
                    double multiplier = 1.0;

                    // Calculation formula
                    double totalEarned = baseIncome * multiplier;

                    // Fixed: Wrap the double value into a BigNumber object to match method signature
                    profile.addMoney(new BigNumber(totalEarned));

                    // Re-render visual text interfaces
                    plugin.getScoreboardManager().updateScoreboard(
                            player,
                            profile.getMoney(),
                            profile.getGems(),
                            profile.getRubies()
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}