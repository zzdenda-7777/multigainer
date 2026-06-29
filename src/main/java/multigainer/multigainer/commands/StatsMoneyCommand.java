package multigainer.multigainer.commands;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.grind.GrindManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.perks.PerkManager;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsMoneyCommand implements CommandExecutor {

    private final Multigainer plugin;

    public StatsMoneyCommand(Multigainer plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Your profile is still loading. Please try again!");
            return true;
        }

        BigNumber upgradeMulti      = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
        BigNumber rebirthBonus      = RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints());
        BigNumber tierBonus         = TierManager.getMultiplierForTier(profile.getTier());
        BigNumber mineMoneyMulti    = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());
        BigNumber farmMulti         = new BigNumber(profile.getFarmMulti());
        BigNumber farmUpgMulti      = UpgradeManager.getFarmTotalMultiplier(profile.getFarmMultiUpgradeLevel());
        BigNumber grindFarmMulti    = new BigNumber(GrindManager.getFarmMulti(profile.getGrindFarmMultiLevel()));
        BigNumber perkMulti         = PerkManager.getTotalPerkMultiplierBig(profile.getPerkCounts());
        double    exponent          = GrindManager.getMoneyExponent(profile.getGrindExponentLevel());

        // Total (before exponent)
        BigNumber totalLinear = upgradeMulti
            .multiply(rebirthBonus)
            .multiply(tierBonus)
            .multiply(mineMoneyMulti)
            .multiply(farmMulti)
            .multiply(farmUpgMulti)
            .multiply(grindFarmMulti)
            .multiply(perkMulti);

        // After exponent
        double log10 = Math.log10(totalLinear.getMantissa()) + totalLinear.getExponent();
        BigNumber totalEarned = UpgradeManager.fromLog10(exponent * log10);

        java.util.UUID uid = player.getUniqueId();
        player.sendMessage(ChatColor.GOLD + "════════ 💰 MONEY MULTIPLIERS 💰 ════════");
        player.sendMessage(fmt(uid, "⚡ Upgrade Multi",    upgradeMulti));
        player.sendMessage(fmt(uid, "🌱 Farm Multi",       farmMulti));
        player.sendMessage(fmt(uid, "🔧 Farm Upgrade",     farmUpgMulti));
        player.sendMessage(fmt(uid, "🏔 Tier Bonus",       tierBonus));
        player.sendMessage(fmt(uid, "💀 Rebirth Bonus",    rebirthBonus));
        player.sendMessage(fmt(uid, "⛏ Mine Level",       mineMoneyMulti));
        player.sendMessage(fmt(uid, "🌾 Grind Farm",       grindFarmMulti));
        player.sendMessage(fmt(uid, "✦ Perk Multi",        perkMulti));
        player.sendMessage(ChatColor.GRAY + "──────────────────────────────────────");
        player.sendMessage(ChatColor.GRAY + "Total Multi: " + ChatColor.WHITE + NumberFormatter.format(totalLinear, uid) + "x");
        player.sendMessage(ChatColor.GRAY + "After Exponent: " + ChatColor.WHITE + NumberFormatter.format(totalEarned, uid) + "/s");
        player.sendMessage(ChatColor.YELLOW + "^ Exponent: §f" + String.format("%.2f", exponent));
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════════");

        return true;
    }

    private static String fmt(java.util.UUID uid, String label, BigNumber value) {
        return ChatColor.GRAY + label + ": " + ChatColor.WHITE + NumberFormatter.format(value, uid) + "x";
    }
}
