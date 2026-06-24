package multigainer.multigainer.scoreboard;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.levels.FarmingLevelManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.scoreboard.fastboard.fastboard.adventure.FastBoard;
import multigainer.multigainer.tier.TierManager;
import multigainer.multigainer.upgrades.UpgradeManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer; // DŮLEŽITÉ
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final Multigainer plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    // Serializer pro převod tvých § kódů na Component
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    public ScoreboardManager(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void createScoreboard(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle(SERIALIZER.deserialize("§6§lMULTIGAINER"));
        boards.put(player.getUniqueId(), board);
    }

    public void removeScoreboard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) board.delete();
    }

    public void updateScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies, int farmLvl, double farmXp, int mineLvl, double mineXp) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        BigNumber totalMoneyMulti = new BigNumber(1.0);
        BigNumber totalGemsMulti = new BigNumber(1.0);

        if (profile != null) {
            BigNumber upgradeMulti = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
            BigNumber rebirthMulti = new BigNumber(RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints()));
            BigNumber tierMulti = new BigNumber(TierManager.getMultiplierForTier(profile.getTier()));
            BigNumber mineMoneyMulti = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());

            totalMoneyMulti = upgradeMulti.multiply(rebirthMulti).multiply(tierMulti).multiply(mineMoneyMulti);
            totalGemsMulti = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel());
        }

        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);

        // Zde používáme serializaci pro každý řádek
        board.updateLines(
                SERIALIZER.deserialize(" "),
                SERIALIZER.deserialize("§a⛃ §8| §aMoney: §f$" + NumberFormatter.format(money) + " §8(" + NumberFormatter.format(totalMoneyMulti) + "×)"),
                SERIALIZER.deserialize("§b✦ §8| §bGems: §f" + NumberFormatter.format(gems) + " §8(" + NumberFormatter.format(totalGemsMulti) + "×)"),
                SERIALIZER.deserialize("§c♦ §8| §cRubies: §f" + NumberFormatter.format(rubies)),
                SERIALIZER.deserialize("  "),
                SERIALIZER.deserialize("§e🌾 §8| §eFarm Lvl: " + farmLvl + " §8(" + (int) farmXp + "/" + (int) reqFarmXp + ")"),
                SERIALIZER.deserialize(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp)),
                SERIALIZER.deserialize("   "),
                SERIALIZER.deserialize("§7⛏ §8| §7Mine Lvl: " + mineLvl + " §8(" + (int) mineXp + "/" + (int) reqMineXp + ")"),
                SERIALIZER.deserialize(MiningLevelManager.generateXpBar(mineXp, reqMineXp)),
                SERIALIZER.deserialize("    "),
                SERIALIZER.deserialize("§f⚡ MSPT: §e" + String.format("%.2f", Bukkit.getAverageTickTime())),
                SERIALIZER.deserialize("§7§oplay.multigainer.net")
        );
    }
}