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
        // Currency
        profile.setMoney(new BigNumber(0));
        profile.setGems(new BigNumber(0));
        profile.setRubies(new BigNumber(0));
        profile.setGrindingPoints(0.0);

        // Core progression
        profile.setUpgradeLevel(0);
        profile.setTier(0);
        profile.setTierPoints(0);
        profile.setFarmingLevel(0);
        profile.setFarmingXp(0.0);
        profile.setMiningLevel(0);
        profile.setMiningXp(0.0);
        profile.setRebirthPoints(0.0);
        profile.setRebirthCount(0);

        // Pickaxe
        profile.setPickaxeTier(0);
        profile.setMiningSpeedLevel(0);
        profile.setXpMultiLevel(0);
        profile.setGemMultiLevel(0);
        for (int i = 0; i < 17; i++) profile.setBlockStorage(i, 0);

        // Farming
        for (int i = 0; i < 7; i++) profile.setSeedStorage(i, 0);
        profile.setFarmMulti(1.0);
        profile.setChosenCrop(0);
        profile.setHoeTier(0);
        profile.setAutoMerge(false);
        for (int i = 0; i < 4; i++) profile.setEnchantMessageEnabled(i, true);
        profile.setLevelUpFarmMessageEnabled(true);

        // Upgrade levels
        profile.setGemUpgradeLevel(0);
        profile.setFarmMultiUpgradeLevel(0);

        // Grinding Points system
        profile.setGrindMessagesEnabled(true);
        profile.setGrindChanceLevel(0);
        profile.setGrindExponentLevel(0);
        profile.setGrindFarmMultiLevel(0);
        profile.setGrindGemMultiLevel(0);
        profile.setGrindFarmXpLevel(0);
        profile.setGrindMineXpLevel(0);
        profile.setGrindSeedMultiLevel(0);
        profile.setGrindGPMultiLevel(0);

        // Perks
        for (int i = 0; i < 5; i++) {
            profile.setPerkCount(i, 0);
            profile.setPerkChanceLevel(i, 0);
            profile.setPerkMessageEnabled(i, true);
        }

        plugin.getPlayerDataManager().saveProfileSynchronously(targetOffline.getUniqueId(), profile);
        sender.sendMessage(ChatColor.GREEN + "✔ Successfully reset ALL data for: " + playerName);
        notifyOnline(targetOnline, profile, "§c⚠ Your stats have been fully reset by an admin!");
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
