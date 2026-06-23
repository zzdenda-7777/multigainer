package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveCommand implements CommandExecutor {
    private final Multigainer plugin;

    public GiveCommand(Multigainer plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("multigainer.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /multigainer give <player> <currency/stat> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        String currency = args[1].toLowerCase();
        double amount = Double.parseDouble(args[2]);
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(target.getUniqueId());

        if (profile == null) {
            sender.sendMessage(ChatColor.RED + "Player profile data not loaded.");
            return true;
        }

        switch (currency) {
            case "money":
                profile.addMoney(new BigNumber(amount));
                break;
            case "gems":
                profile.addGems(new BigNumber(amount));
                break;
            case "rubies":
                profile.addRubies(new BigNumber(amount));
                break;
            case "tier":
                // Allows direct admin tier overrides and resets (e.g., setting to 0)
                profile.setTier((int) amount);
                break;
            case "rebirth":
                // Allows direct admin rebirth point overrides and resets (e.g., setting to 0)
                profile.setRebirthPoints(amount);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid type! (money, gems, rubies, tier, rebirth)");
                return true;
        }

        sender.sendMessage(ChatColor.GREEN + "Set/Added " + amount + " " + currency + " for " + target.getName());
        return true;
    }
}