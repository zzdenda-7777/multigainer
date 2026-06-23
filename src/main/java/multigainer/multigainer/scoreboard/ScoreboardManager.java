package multigainer.multigainer.scoreboard;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.FarmingLevelManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager {

    private final Multigainer plugin;

    // Added constructor hook to safely fetch profile data without breaking signatures
    public ScoreboardManager(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void createScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies, int farmLvl, double farmXp, int mineLvl, double mineXp) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("currency_sb", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "MULTIGAINER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(" ").setScore(14);

        // Calculate compounding incremental game multipliers dynamically
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        BigNumber totalMoneyMulti = new BigNumber(1.0);
        BigNumber totalGemsMulti = new BigNumber(1.0);

        if (profile != null) {
            BigNumber upgradeMulti = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
            BigNumber rebirthMulti = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
            BigNumber tierMulti = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));
            BigNumber mineMoneyMulti = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

            // Money Multipliers stack exponentially (Upgrades * Rebirth * Tier * Mine Money Multiplier)
            totalMoneyMulti = upgradeMulti.multiply(rebirthMulti).multiply(tierMulti).multiply(mineMoneyMulti);
            // Gems Multipliers scale by mining tier progression
            totalGemsMulti = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel());
        }

        // Economy Section
        Team moneyTeam = board.registerNewTeam("sb_money");
        moneyTeam.addEntry(ChatColor.GREEN.toString());
        moneyTeam.setPrefix("§a⛃ §8| §aMoney: §f$");
        moneyTeam.setSuffix(NumberFormatter.format(money) + " §8(x" + NumberFormatter.format(totalMoneyMulti) + ")");
        objective.getScore(ChatColor.GREEN.toString()).setScore(13);

        Team gemsTeam = board.registerNewTeam("sb_gems");
        gemsTeam.addEntry(ChatColor.AQUA.toString());
        gemsTeam.setPrefix("§b✦ §8| §bGems: §f");
        gemsTeam.setSuffix(NumberFormatter.format(gems) + " §8(x" + NumberFormatter.format(totalGemsMulti) + ")");
        objective.getScore(ChatColor.AQUA.toString()).setScore(12);

        Team rubiesTeam = board.registerNewTeam("sb_rubies");
        rubiesTeam.addEntry(ChatColor.RED.toString());
        rubiesTeam.setPrefix("§c♦ §8| §cRubies: §f");
        rubiesTeam.setSuffix(NumberFormatter.format(rubies));
        objective.getScore(ChatColor.RED.toString()).setScore(11);

        objective.getScore("  ").setScore(10);

        // Progression Panels
        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        Team farmLvlTeam = board.registerNewTeam("sb_farmlvl");
        farmLvlTeam.addEntry(ChatColor.GOLD.toString());
        farmLvlTeam.setPrefix("§e🌾 §8| §eFarm Lvl: ");
        farmLvlTeam.setSuffix(farmLvl + " §8(" + (int) farmXp + "/" + (int) reqFarmXp + ")");
        objective.getScore(ChatColor.GOLD.toString()).setScore(9);

        Team farmXpTeam = board.registerNewTeam("sb_farmxp");
        farmXpTeam.addEntry(ChatColor.LIGHT_PURPLE.toString());
        farmXpTeam.setPrefix("  ");
        farmXpTeam.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));
        objective.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(8);

        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        Team mineLvlTeam = board.registerNewTeam("sb_minelvl");
        mineLvlTeam.addEntry(ChatColor.BLUE.toString());
        mineLvlTeam.setPrefix("§7⛏ §8| §7Mine Lvl: ");
        mineLvlTeam.setSuffix(mineLvl + " §8(" + (int) mineXp + "/" + (int) reqMineXp + ")");
        objective.getScore(ChatColor.BLUE.toString()).setScore(7);

        Team mineXpTeam = board.registerNewTeam("sb_minexp");
        mineXpTeam.addEntry(ChatColor.DARK_PURPLE.toString());
        mineXpTeam.setPrefix("  ");
        mineXpTeam.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));
        objective.getScore(ChatColor.DARK_PURPLE.toString()).setScore(6);

        // Temporary Debug MSPT
        objective.getScore("   ").setScore(3);
        Team msptTeam = board.registerNewTeam("sb_mspt");
        msptTeam.addEntry(ChatColor.WHITE.toString());
        msptTeam.setPrefix("§f⚡ MSPT: §e");
        msptTeam.setSuffix(String.format("%.2f", Bukkit.getAverageTickTime()));
        objective.getScore(ChatColor.WHITE.toString()).setScore(2);

        objective.getScore("§7§oplay.multigainer.net").setScore(1);

        player.setScoreboard(board);
    }

    public void updateScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies, int farmLvl, double farmXp, int mineLvl, double mineXp) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("currency_sb") == null) return;

        // Recalculate compounding incremental multipliers on runtime updates
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        BigNumber totalMoneyMulti = new BigNumber(1.0);
        BigNumber totalGemsMulti = new BigNumber(1.0);

        if (profile != null) {
            BigNumber upgradeMulti = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
            BigNumber rebirthMulti = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
            BigNumber tierMulti = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));
            BigNumber mineMoneyMulti = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

            // Money Multipliers stack exponentially (Upgrades * Rebirth * Tier * Mine Money Multiplier)
            totalMoneyMulti = upgradeMulti.multiply(rebirthMulti).multiply(tierMulti).multiply(mineMoneyMulti);
            totalGemsMulti = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel());
        }

        // Update Economy Display Elements
        Team moneyTeam = board.getTeam("sb_money");
        if (moneyTeam != null) {
            moneyTeam.setSuffix(NumberFormatter.format(money) + " §7(x" + NumberFormatter.format(totalMoneyMulti) + ")");
        }

        Team gemsTeam = board.getTeam("sb_gems");
        if (gemsTeam != null) {
            gemsTeam.setSuffix(NumberFormatter.format(gems) + " §7(x" + NumberFormatter.format(totalGemsMulti) + ")");
        }

        Team rubiesTeam = board.getTeam("sb_rubies");
        if (rubiesTeam != null) rubiesTeam.setSuffix(NumberFormatter.format(rubies));

        // Update Farming Displays
        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        Team farmLvlTeam = board.getTeam("sb_farmlvl");
        if (farmLvlTeam != null) farmLvlTeam.setSuffix(farmLvl + " §7(" + (int) farmXp + "/" + (int) reqFarmXp + ")");
        Team farmXpTeam = board.getTeam("sb_farmxp");
        if (farmXpTeam != null) farmXpTeam.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));

        // Update Mining Displays
        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        Team mineLvlTeam = board.getTeam("sb_minelvl");
        if (mineLvlTeam != null) mineLvlTeam.setSuffix(mineLvl + " §7(" + (int) mineXp + "/" + (int) reqMineXp + ")");
        Team mineXpTeam = board.getTeam("sb_minexp");
        if (mineXpTeam != null) mineXpTeam.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));

        // Update Performance Metrics
        Team msptTeam = board.getTeam("sb_mspt");
        if (msptTeam != null) {
            msptTeam.setSuffix(String.format("%.2f", Bukkit.getAverageTickTime()));
        }
    }
}