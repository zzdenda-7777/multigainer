package multigainer.multigainer.scoreboard;

import multigainer.multigainer.formatting.NumberFormatter;
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

    public void createScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("currency_sb", Criteria.DUMMY,
                ChatColor.GOLD + "" + ChatColor.BOLD + "MULTIGAINER");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        objective.getScore(" ").setScore(6);

        Team moneyTeam = board.registerNewTeam("sb_money");
        moneyTeam.addEntry(ChatColor.GREEN.toString());
        moneyTeam.setPrefix(ChatColor.DARK_GREEN + "● Money: " + ChatColor.WHITE + "$");
        moneyTeam.setSuffix(NumberFormatter.format(money));
        objective.getScore(ChatColor.GREEN.toString()).setScore(5);

        Team gemsTeam = board.registerNewTeam("sb_gems");
        gemsTeam.addEntry(ChatColor.AQUA.toString());
        gemsTeam.setPrefix(ChatColor.DARK_AQUA + "● Gems: " + ChatColor.WHITE);
        gemsTeam.setSuffix(NumberFormatter.format(gems));
        objective.getScore(ChatColor.AQUA.toString()).setScore(4);

        Team rubiesTeam = board.registerNewTeam("sb_rubies");
        rubiesTeam.addEntry(ChatColor.RED.toString());
        rubiesTeam.setPrefix(ChatColor.DARK_RED + "● Rubies: " + ChatColor.WHITE);
        rubiesTeam.setSuffix(NumberFormatter.format(rubies));
        objective.getScore(ChatColor.RED.toString()).setScore(3);

        objective.getScore("  ").setScore(2);
        objective.getScore(ChatColor.YELLOW + "play.multigainer.net").setScore(1);

        player.setScoreboard(board);
    }

    public void updateScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("currency_sb") == null) return;

        Team moneyTeam = board.getTeam("sb_money");
        if (moneyTeam != null) moneyTeam.setSuffix(NumberFormatter.format(money));

        Team gemsTeam = board.getTeam("sb_gems");
        if (gemsTeam != null) gemsTeam.setSuffix(NumberFormatter.format(gems));

        Team rubiesTeam = board.getTeam("sb_rubies");
        if (rubiesTeam != null) rubiesTeam.setSuffix(NumberFormatter.format(rubies));
    }
}