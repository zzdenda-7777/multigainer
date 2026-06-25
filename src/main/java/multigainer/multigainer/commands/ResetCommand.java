package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetCommand implements CommandExecutor {
    private final Multigainer plugin;

    public ResetCommand(Multigainer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("multigainer.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /multigainer reset <player>");
            sender.sendMessage(ChatColor.RED + "       /multigainer reset <player> <currency> <amount>");
            sender.sendMessage(ChatColor.GRAY + "Currencies: money, gems, rubies, grinding_points, tier, rebirth");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(playerName);
        Player targetOnline = Bukkit.getPlayer(playerName);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(targetOffline.getUniqueId());
        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile not found for: " + playerName);
            return true;
        }

        // /multigainer reset <player> <currency> <amount>
        if (args.length >= 4) {
            String currency = args[2].toLowerCase();
            double amount;
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                return true;
            }

            switch (currency) {
                case "money"           -> profile.setMoney(new BigNumber(amount));
                case "gems"            -> profile.setGems(new BigNumber(amount));
                case "rubies"          -> profile.setRubies(new BigNumber(amount));
                case "grinding_points", "gp" -> profile.setGrindingPoints(amount);
                case "tier"            -> profile.setTier((int) amount);
                case "rebirth"         -> profile.setRebirthPoints(amount);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown currency: " + currency);
                    sender.sendMessage(ChatColor.GRAY + "Valid: money, gems, rubies, grinding_points, tier, rebirth");
                    return true;
                }
            }

            plugin.getPlayerDataManager().saveProfileSynchronously(targetOffline.getUniqueId(), profile);
            sender.sendMessage(ChatColor.GREEN + "✔ Set " + playerName + "'s " + currency + " to " + amount);
            notifyOnline(targetOnline, profile, "§e⚠ Admin set your " + currency + " to " + (long)amount + "!");
            return true;
        }

        // /multigainer reset <player> — full reset to defaults
        profile.setMoney(new BigNumber(0));
        profile.setGems(new BigNumber(0));
        profile.setRubies(new BigNumber(0));
        profile.setGrindingPoints(0.0);
        profile.setUpgradeLevel(0);
        profile.setTier(0);
        profile.setFarmingLevel(1);
        profile.setFarmingXp(0.0);
        profile.setMiningLevel(1);
        profile.setMiningXp(0.0);
        profile.setRebirthPoints(0.0);
        profile.setTierPoints(0);
        profile.setRebirthCount(0);

        plugin.getPlayerDataManager().saveProfileSynchronously(targetOffline.getUniqueId(), profile);
        sender.sendMessage(ChatColor.GREEN + "✔ Successfully reset all stats for: " + playerName);
        notifyOnline(targetOnline, profile, "§c⚠ Your stats have been reset by an admin!");
        return true;
    }

    private void notifyOnline(Player target, PlayerProfile profile, String msg) {
        if (target == null || !target.isOnline()) return;
        target.sendMessage(msg);
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(target,
                    profile.getMoney(), profile.getGems(), profile.getRubies(),
                    profile.getFarmingLevel(), profile.getFarmingXp(),
                    profile.getMiningLevel(), profile.getMiningXp());
        }
    }
}
