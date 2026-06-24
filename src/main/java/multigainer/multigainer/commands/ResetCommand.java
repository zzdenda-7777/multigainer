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
            return true;
        }

        String playerName = args[1];
        playerName = playerName.substring(0, 1).toUpperCase() + playerName.substring(1);
        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(playerName);
        Player targetOnline = Bukkit.getPlayer(playerName);

        // Pokus najít hráče v cache
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(targetOffline.getUniqueId());

        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile not found for: " + playerName);
            return true;
        }

        // Reset všech statistik na výchozí hodnoty
        profile.setMoney(new BigNumber(0));
        profile.setGems(new BigNumber(0));
        profile.setRubies(new BigNumber(0));
        profile.setUpgradeLevel(0);
        profile.setTier(0);
        profile.setFarmingLevel(0);
        profile.setFarmingXp(0.0);
        profile.setMiningLevel(0);
        profile.setMiningXp(0.0);
        profile.setRebirthPoints(0.0);
        profile.setTierPoints(0);
        profile.setRebirthCount(0);

        // Ulož změny na disk
        plugin.getPlayerDataManager().saveProfileSynchronously(targetOffline.getUniqueId(), profile);

        sender.sendMessage(ChatColor.GREEN + "✔ Successfully reset all stats for: " + playerName);

        // Pokud je hráč online, aktualizuj jeho scoreboard
        if (targetOnline != null && targetOnline.isOnline()) {
            if (plugin.getScoreboardManager() != null) {
                plugin.getScoreboardManager().updateScoreboard(
                        targetOnline,
                        profile.getMoney(),
                        profile.getGems(),
                        profile.getRubies(),
                        profile.getFarmingLevel(),
                        profile.getFarmingXp(),
                        profile.getMiningLevel(),
                        profile.getMiningXp()
                );
            }
            targetOnline.sendMessage(ChatColor.RED + "⚠ Your stats have been reset by an admin!");
        }

        return true;
    }
}