package multigainer.multigainer.income;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.levels.MiningLevelManager; // IMPORTED
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
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
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player == null || !player.isOnline()) continue; // Pojistka
                    PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
                    if (profile == null) continue;

                    // 1. Base Configuration Value
                    BigNumber baseIncome = new BigNumber(1.0);

                    // 2. Upgrades Multiplier
                    BigNumber upgradeMultiplier = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());

                    // 3. Rebirth Multiplier Hook
                    double rebirthBonus = RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints());
                    BigNumber rebirthMultiplier = new BigNumber(rebirthBonus);

                    // 4. Tier Multiplier Hook
                    double tierBonus = TierManager.getMultiplierForTier(profile.getTier());
                    BigNumber tierMultiplier = new BigNumber(tierBonus);

                    // 5. Mining Multiplier Hook
                    BigNumber mineMoneyMultiplier = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

                    // 6. Farm Multi (accumulated from crops) and farm upgrade multiplier
                    BigNumber farmMultiplier    = new BigNumber(profile.getFarmMulti());
                    BigNumber farmUpgMultiplier = multigainer.multigainer.upgrades.UpgradeManager.getFarmTotalMultiplier(profile.getFarmMultiUpgradeLevel());

                    // Compound Multiplier Formula Stacking
                    BigNumber totalEarned = baseIncome
                            .multiply(upgradeMultiplier)
                            .multiply(rebirthMultiplier)
                            .multiply(tierMultiplier)
                            .multiply(mineMoneyMultiplier)
                            .multiply(farmMultiplier)
                            .multiply(farmUpgMultiplier);

                    profile.setMoney(profile.getMoney().add(totalEarned));

                    // Re-render visual text interfaces instantly for the player
                    if (plugin.getScoreboardManager() != null) {
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
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}