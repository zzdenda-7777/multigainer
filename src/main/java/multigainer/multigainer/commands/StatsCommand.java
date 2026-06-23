package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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
        // Ensure only players can run this command
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can look up their stats!");
            return true;
        }

        Player player = (Player) sender;

        // 1. Fetch the player's profile object directly from the PlayerDataManager
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        // Safety check to ensure their data has fully loaded from SQLite
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Your player profile is still loading. Please try again in a moment!");
            return true;
        }

        // 2. Safely extract the mining level from the loaded profile object
        int miningLevel = profile.getMiningLevel();

        // 3. Calculate the multipliers exponentially: base^level
        double moneyMultiplier = Math.pow(1.1, miningLevel);
        double gemMultiplier = Math.pow(1.02, miningLevel);

        // 4. Format the multipliers cleanly to 2 decimal places so it looks nice in chat
        String formattedMoney = String.format("%.2f", moneyMultiplier);
        String formattedGems = String.format("%.2f", gemMultiplier);

        // 5. Send the beautifully formatted breakdown to the player
        player.sendMessage(ChatColor.GOLD + "========== ✨ YOUR STATS ✨ ==========");
        player.sendMessage(ChatColor.YELLOW + "⛏ Mining Level: " + ChatColor.WHITE + miningLevel);
        player.sendMessage(ChatColor.GREEN + "💵 Money Multiplier: " + ChatColor.WHITE + formattedMoney + "x");
        player.sendMessage(ChatColor.AQUA + "💎 Gem Multiplier: " + ChatColor.WHITE + formattedGems + "x");
        player.sendMessage(ChatColor.GOLD + "====================================");

        return true;
    }
}