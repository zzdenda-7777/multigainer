package multigainer.multigainer.income;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;
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
                    if (profile == null) continue;

                    // 1. Base Configuration Value (Using BigNumber to avoid double limitations)
                    BigNumber baseIncome = new BigNumber(1.0);

                    // 2. Multipliers (Currently hooks into your new compounding 27-tier system!)
                    BigNumber upgradeMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());

                    /* * 💡 HOW TO EXPAND THIS IN THE FUTURE:
                     * When you add new systems, you can define them here like this:
                     * BigNumber petMultiplier = profile.getPetMultiplier();
                     * BigNumber rebirthMultiplier = profile.getRebirthMultiplier();
                     */

                    // 3. Calculation Formula Stacking
                    BigNumber totalEarned = baseIncome.multiply(upgradeMultiplier);

                    /* * To add more multipliers to the final product later, just chain them:
                     * totalEarned = totalEarned.multiply(petMultiplier).multiply(rebirthMultiplier);
                     */

                    // Add the money directly to the profile wrapper
                    profile.addMoney(totalEarned);

                    // Re-render visual text interfaces instantly for the player
                    plugin.getScoreboardManager().updateScoreboard(
                            player,
                            profile.getMoney(),
                            profile.getGems(),
                            profile.getRubies(),
                            profile.getFarmingLevel(),
                            profile.getFarmingXp(),
                            profile.getMiningLevel(),
                            profile.getMiningXp()
                    );
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}