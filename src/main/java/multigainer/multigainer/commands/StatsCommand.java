package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.MiningLevelManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    private final Multigainer plugin;

    public StatsCommand(Multigainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure only players can execute this command
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        // Safety check to ensure data has loaded
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Your profile is still loading, please try again in a moment.");
            return true;
        }

        // Retrieve current mining level
        int miningLevel = profile.getMiningLevel();

        // Calculate and format multipliers using the MiningLevelManager and NumberFormatter
        String moneyMulti = NumberFormatter.format(MiningLevelManager.getMoneyMultiplier(miningLevel));
        String gemsMulti = NumberFormatter.format(MiningLevelManager.getGemsMultiplier(miningLevel));

        // Display formatted statistics
        player.sendMessage(ChatColor.GOLD + "========== ✨ YOUR STATS ✨ ==========");
        player.sendMessage(ChatColor.YELLOW + "⛏ Mining Level: " + ChatColor.WHITE + miningLevel);
        player.sendMessage(ChatColor.GREEN + "💵 Money Multiplier: " + ChatColor.WHITE + moneyMulti + "x");
        player.sendMessage(ChatColor.AQUA + "💎 Gem Multiplier: " + ChatColor.WHITE + gemsMulti + "x");
        player.sendMessage(ChatColor.GOLD + "======================================");

        return true;
    }
}