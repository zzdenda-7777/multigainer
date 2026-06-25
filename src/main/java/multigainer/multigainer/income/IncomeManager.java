package multigainer.multigainer.income;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.grind.GrindManager;
import multigainer.multigainer.levels.MiningLevelManager;
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
                    if (player == null || !player.isOnline()) continue;
                    PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
                    if (profile == null) continue;

                    // All money multipliers combined
                    BigNumber upgradeMultiplier  = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
                    double rebirthBonus          = RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints());
                    double tierBonus             = TierManager.getMultiplierForTier(profile.getTier());
                    BigNumber mineMoneyMultiplier = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());
                    BigNumber farmMultiplier      = new BigNumber(profile.getFarmMulti());
                    BigNumber farmUpgMultiplier   = UpgradeManager.getFarmTotalMultiplier(profile.getFarmMultiUpgradeLevel());
                    BigNumber grindFarmMultiplier = new BigNumber(GrindManager.getFarmMulti(profile.getGrindFarmMultiLevel()));

                    BigNumber allMulti = upgradeMultiplier
                            .multiply(new BigNumber(rebirthBonus))
                            .multiply(new BigNumber(tierBonus))
                            .multiply(mineMoneyMultiplier)
                            .multiply(farmMultiplier)
                            .multiply(farmUpgMultiplier)
                            .multiply(grindFarmMultiplier);

                    // Apply money exponent: totalEarned = allMulti ^ exponent
                    double exponent = GrindManager.getMoneyExponent(profile.getGrindExponentLevel());
                    double log10 = Math.log10(allMulti.getMantissa()) + allMulti.getExponent();
                    BigNumber totalEarned = UpgradeManager.fromLog10(exponent * log10);

                    profile.setMoney(profile.getMoney().add(totalEarned));

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
