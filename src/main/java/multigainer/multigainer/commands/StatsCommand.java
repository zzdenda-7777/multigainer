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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Your profile is still loading. Please try again in a moment!");
            return true;
        }

        int miningLevel = profile.getMiningLevel();

        // Using your NumberFormatter for consistency across the plugin
        java.util.UUID uid = player.getUniqueId();
        String formattedMoney = NumberFormatter.format(MiningLevelManager.getMoneyMultiplier(miningLevel), uid);
        String formattedGems = NumberFormatter.format(MiningLevelManager.getGemsMultiplier(miningLevel), uid);

        player.sendMessage(ChatColor.GOLD + "========== ✨ YOUR STATS ✨ ==========");
        player.sendMessage(ChatColor.YELLOW + "⛏ Mining Level: " + ChatColor.WHITE + miningLevel);
        player.sendMessage(ChatColor.GREEN + "💵 Money Multiplier: " + ChatColor.WHITE + formattedMoney + "x");
        player.sendMessage(ChatColor.AQUA + "💎 Gem Multiplier: " + ChatColor.WHITE + formattedGems + "x");
        player.sendMessage(ChatColor.GOLD + "====================================");

        return true;
    }
}