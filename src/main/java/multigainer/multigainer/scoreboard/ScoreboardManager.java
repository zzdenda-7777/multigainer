package multigainer.multigainer.scoreboard;

import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.FarmingLevelManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager {

    public void createScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies, int farmLvl, double farmXp, int mineLvl, double mineXp) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("currency_sb", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "MULTIGAINER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(" ").setScore(12);

        // Economy Section
        Team moneyTeam = board.registerNewTeam("sb_money");
        moneyTeam.addEntry(ChatColor.GREEN.toString());
        moneyTeam.setPrefix("§a⛃ §8| §aMoney: §f$");
        moneyTeam.setSuffix(NumberFormatter.format(money));
        objective.getScore(ChatColor.GREEN.toString()).setScore(11);

        Team gemsTeam = board.registerNewTeam("sb_gems");
        gemsTeam.addEntry(ChatColor.AQUA.toString());
        gemsTeam.setPrefix("§b✦ §8| §bGems: §f");
        gemsTeam.setSuffix(NumberFormatter.format(gems));
        objective.getScore(ChatColor.AQUA.toString()).setScore(10);

        Team rubiesTeam = board.registerNewTeam("sb_rubies");
        rubiesTeam.addEntry(ChatColor.RED.toString());
        rubiesTeam.setPrefix("§c♦ §8| §cRubies: §f");
        rubiesTeam.setSuffix(NumberFormatter.format(rubies));
        objective.getScore(ChatColor.RED.toString()).setScore(9);

        objective.getScore("  ").setScore(8);

        // Professional Progression Panels
        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        Team farmLvlTeam = board.registerNewTeam("sb_farmlvl");
        farmLvlTeam.addEntry(ChatColor.GOLD.toString());
        farmLvlTeam.setPrefix("§e🌾 §8| §eFarm Lvl: §f");
        farmLvlTeam.setSuffix(farmLvl + " §7(" + (int) farmXp + "/" + (int) reqFarmXp + ")");
        objective.getScore(ChatColor.GOLD.toString()).setScore(7);

        Team farmXpTeam = board.registerNewTeam("sb_farmxp");
        farmXpTeam.addEntry(ChatColor.LIGHT_PURPLE.toString());
        farmXpTeam.setPrefix("  ");
        farmXpTeam.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));
        objective.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(6);

        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        Team mineLvlTeam = board.registerNewTeam("sb_minelvl");
        mineLvlTeam.addEntry(ChatColor.BLUE.toString());
        mineLvlTeam.setPrefix("§7⛏ §8| §7Mine Lvl: §f");
        mineLvlTeam.setSuffix(mineLvl + " §7(" + (int) mineXp + "/" + (int) reqMineXp + ")");
        objective.getScore(ChatColor.BLUE.toString()).setScore(5);

        Team mineXpTeam = board.registerNewTeam("sb_minexp");
        mineXpTeam.addEntry(ChatColor.DARK_PURPLE.toString());
        mineXpTeam.setPrefix("  ");
        mineXpTeam.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));
        objective.getScore(ChatColor.DARK_PURPLE.toString()).setScore(4);

        objective.getScore("   ").setScore(2);
        objective.getScore("§7§oplay.multigainer.net").setScore(1);

        player.setScoreboard(board);
    }

    public void updateScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies, int farmLvl, double farmXp, int mineLvl, double mineXp) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("currency_sb") == null) return;

        Team moneyTeam = board.getTeam("sb_money");
        if (moneyTeam != null) moneyTeam.setSuffix(NumberFormatter.format(money));

        Team gemsTeam = board.getTeam("sb_gems");
        if (gemsTeam != null) gemsTeam.setSuffix(NumberFormatter.format(gems));

        Team rubiesTeam = board.getTeam("sb_rubies");
        if (rubiesTeam != null) rubiesTeam.setSuffix(NumberFormatter.format(rubies));

        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        Team farmLvlTeam = board.getTeam("sb_farmlvl");
        if (farmLvlTeam != null) farmLvlTeam.setSuffix(farmLvl + " §7(" + (int) farmXp + "/" + (int) reqFarmXp + ")");
        Team farmXpTeam = board.getTeam("sb_farmxp");
        if (farmXpTeam != null) farmXpTeam.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));

        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        Team mineLvlTeam = board.getTeam("sb_minelvl");
        if (mineLvlTeam != null) mineLvlTeam.setSuffix(mineLvl + " §7(" + (int) mineXp + "/" + (int) reqMineXp + ")");
        Team mineXpTeam = board.getTeam("sb_minexp");
        if (mineXpTeam != null) mineXpTeam.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));
    }
}