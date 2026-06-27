package multigainer.multigainer.scoreboard;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.armor.ArmorManager;
import multigainer.multigainer.armor.ArmorType;
import multigainer.multigainer.artifacts.ArtifactManager;
import multigainer.multigainer.artifacts.ArtifactType;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.farming.FarmingManager;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.grind.GrindManager;
import multigainer.multigainer.levels.FarmingLevelManager;
import multigainer.multigainer.levels.MiningLevelManager;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.rebirth.RebirthManager;
import multigainer.multigainer.tier.TierManager;
import multigainer.multigainer.perks.PerkManager;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ScoreboardManager {

    private final Multigainer plugin;

    public ScoreboardManager(Multigainer plugin) {
        this.plugin = plugin;
    }

    public void createScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies,
                                 int farmLvl, double farmXp, int mineLvl, double mineXp) {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getNewScoreboard();

        String title = "§x§F§F§D§7§0§0§lM§x§F§F§D§7§0§0§lU§x§F§D§C§9§0§B§lL" +
                "§x§F§C§B§C§1§6§lT§x§F§A§A§F§1§C§lI§x§E§D§9§E§4§4§lG" +
                "§x§F§A§A§F§1§C§lA§x§F§C§B§C§1§6§lI§x§F§D§C§9§0§B§lN" +
                "§x§F§F§D§7§0§0§lE§x§F§F§D§7§0§0§lR 2";

        Objective objective = board.registerNewObjective("currency_sb", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
        objective.getScore(" ").setScore(14);

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        BigNumber[] multis = calcMultipliers(profile);
        BigNumber totalMoneyMulti = multis[0];
        BigNumber totalGemsMulti  = multis[1];

        // ── Economy ───────────────────────────────────────────────
        java.util.UUID uid = player.getUniqueId();

        Team moneyTeam = board.registerNewTeam("sb_money");
        moneyTeam.addEntry(ChatColor.GREEN.toString());
        moneyTeam.setPrefix("§a⛃ §8│ §aMoney§8: §f$");
        moneyTeam.setSuffix(NumberFormatter.format(money, uid) + " §8(§ax" + NumberFormatter.format(totalMoneyMulti, uid) + "§8)");
        objective.getScore(ChatColor.GREEN.toString()).setScore(13);

        Team gemsTeam = board.registerNewTeam("sb_gems");
        gemsTeam.addEntry(ChatColor.AQUA.toString());
        gemsTeam.setPrefix("§b✦ §8│ §bGems§8: §f");
        gemsTeam.setSuffix(NumberFormatter.format(gems, uid) + " §8(§bx" + NumberFormatter.format(totalGemsMulti, uid) + "§8)");
        objective.getScore(ChatColor.AQUA.toString()).setScore(12);

        Team rubiesTeam = board.registerNewTeam("sb_rubies");
        rubiesTeam.addEntry(ChatColor.RED.toString());
        rubiesTeam.setPrefix("§4♦ §8│ §4Rubies§8: §f");
        rubiesTeam.setSuffix(NumberFormatter.format(rubies, uid));
        objective.getScore(ChatColor.RED.toString()).setScore(11);

        objective.getScore("  ").setScore(10);

        // ── Farming ───────────────────────────────────────────────
        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        Team farmLvlTeam = board.registerNewTeam("sb_farmlvl");
        farmLvlTeam.addEntry(ChatColor.GOLD.toString());
        farmLvlTeam.setPrefix("§e🌾 §8│ §eFarm Lvl§8: ");
        farmLvlTeam.setSuffix(NumberFormatter.format(new BigNumber(farmLvl), uid)
                + " §8(§e" + NumberFormatter.format(new BigNumber(farmXp), uid)
                + "§8/§e" + NumberFormatter.format(new BigNumber(reqFarmXp), uid) + "§8)");
        objective.getScore(ChatColor.GOLD.toString()).setScore(9);

        Team farmXpTeam = board.registerNewTeam("sb_farmxp");
        farmXpTeam.addEntry(ChatColor.LIGHT_PURPLE.toString());
        farmXpTeam.setPrefix("  ");
        farmXpTeam.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));
        objective.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(8);

        // ── Mining ────────────────────────────────────────────────
        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        Team mineLvlTeam = board.registerNewTeam("sb_minelvl");
        mineLvlTeam.addEntry(ChatColor.BLUE.toString());
        mineLvlTeam.setPrefix("§7⛏ §8│ §7Mine Lvl§8: ");
        mineLvlTeam.setSuffix(NumberFormatter.format(new BigNumber(mineLvl), uid)
                + " §8(§7" + NumberFormatter.format(new BigNumber(mineXp), uid)
                + "§8/§7" + NumberFormatter.format(new BigNumber(reqMineXp), uid) + "§8)");
        objective.getScore(ChatColor.BLUE.toString()).setScore(7);

        Team mineXpTeam = board.registerNewTeam("sb_minexp");
        mineXpTeam.addEntry(ChatColor.DARK_PURPLE.toString());
        mineXpTeam.setPrefix("  ");
        mineXpTeam.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));
        objective.getScore(ChatColor.DARK_PURPLE.toString()).setScore(6);

        // ── Farm Multi ────────────────────────────────────────────
        double farmMulti = profile != null ? profile.getFarmMulti() : 1.0;
        int farmUpgLvl   = profile != null ? profile.getFarmMultiUpgradeLevel() : 0;
        BigNumber farmUpgBig = UpgradeManager.getFarmTotalMultiplier(farmUpgLvl);
        String farmMultiSuffix = NumberFormatter.format(new BigNumber(farmMulti), uid) + "x"
                + (farmUpgLvl > 0 ? " §8(§e" + NumberFormatter.format(farmUpgBig, uid) + "x§8)" : "");
        Team farmMultiTeam = board.registerNewTeam("sb_farmmulti");
        farmMultiTeam.addEntry(ChatColor.YELLOW.toString());
        farmMultiTeam.setPrefix("§e🌾 §8│ §eFarm Multi§8: §f");
        farmMultiTeam.setSuffix(farmMultiSuffix);
        objective.getScore(ChatColor.YELLOW.toString()).setScore(5);

        // ── Performance ───────────────────────────────────────────
        objective.getScore("   ").setScore(3);
        Team msptTeam = board.registerNewTeam("sb_mspt");
        msptTeam.addEntry(ChatColor.WHITE.toString());
        msptTeam.setPrefix("§f⚡ MSPT§8: §e");
        msptTeam.setSuffix(String.format("%.2f", Bukkit.getAverageTickTime()));
        objective.getScore(ChatColor.WHITE.toString()).setScore(2);

        objective.getScore("§7§oplay.multigainer.net").setScore(1);

        player.setScoreboard(board);
    }

    public void updateScoreboard(Player player, BigNumber money, BigNumber gems, BigNumber rubies,
                                 int farmLvl, double farmXp, int mineLvl, double mineXp) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("currency_sb") == null) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        BigNumber[] multis = calcMultipliers(profile);
        BigNumber totalMoneyMulti = multis[0];
        BigNumber totalGemsMulti  = multis[1];

        java.util.UUID uid = player.getUniqueId();
        Team t;
        t = board.getTeam("sb_money");
        if (t != null) t.setSuffix(NumberFormatter.format(money, uid) + " §8(§ax" + NumberFormatter.format(totalMoneyMulti, uid) + "§8)");

        t = board.getTeam("sb_gems");
        if (t != null) t.setSuffix(NumberFormatter.format(gems, uid) + " §8(§bx" + NumberFormatter.format(totalGemsMulti, uid) + "§8)");

        t = board.getTeam("sb_rubies");
        if (t != null) t.setSuffix(NumberFormatter.format(rubies, uid));

        double reqFarmXp = FarmingLevelManager.getRequiredXpForNextLevel(farmLvl);
        t = board.getTeam("sb_farmlvl");
        if (t != null) t.setSuffix(NumberFormatter.format(new BigNumber(farmLvl), uid)
                + " §8(§e" + NumberFormatter.format(new BigNumber(farmXp), uid)
                + "§8/§e" + NumberFormatter.format(new BigNumber(reqFarmXp), uid) + "§8)");
        t = board.getTeam("sb_farmxp");
        if (t != null) t.setSuffix(FarmingLevelManager.generateXpBar(farmXp, reqFarmXp));

        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        t = board.getTeam("sb_minelvl");
        if (t != null) t.setSuffix(NumberFormatter.format(new BigNumber(mineLvl), uid)
                + " §8(§7" + NumberFormatter.format(new BigNumber(mineXp), uid)
                + "§8/§7" + NumberFormatter.format(new BigNumber(reqMineXp), uid) + "§8)");
        t = board.getTeam("sb_minexp");
        if (t != null) t.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));

        t = board.getTeam("sb_farmmulti");
        if (t != null && profile != null) {
            int fUpgLvl = profile.getFarmMultiUpgradeLevel();
            BigNumber fUpgBig = UpgradeManager.getFarmTotalMultiplier(fUpgLvl);
            String fSuffix = NumberFormatter.format(new BigNumber(profile.getFarmMulti()), uid) + "x"
                    + (fUpgLvl > 0 ? " §8(§e" + NumberFormatter.format(fUpgBig, uid) + "x§8)" : "");
            t.setSuffix(fSuffix);
        }

        t = board.getTeam("sb_mspt");
        if (t != null) t.setSuffix(String.format("%.2f", Bukkit.getAverageTickTime()));
    }

    public void updateGemsOnly(Player player, BigNumber gems, int mineLvl, double mineXp) {
        Scoreboard board = player.getScoreboard();
        if (board.getObjective("currency_sb") == null) return;

        java.util.UUID uid = player.getUniqueId();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(uid);
        double grindGemMulti = profile != null ? GrindManager.getGemMulti(profile.getGrindGemMultiLevel()) : 1.0;
        BigNumber totalGemsMulti = profile != null
                ? MiningLevelManager.getGemsMultiplier(profile.getMiningLevel()).multiply(new BigNumber(grindGemMulti))
                : new BigNumber(1.0);

        Team t = board.getTeam("sb_gems");
        if (t != null) t.setSuffix(NumberFormatter.format(gems, uid) + " §8(§bx" + NumberFormatter.format(totalGemsMulti, uid) + "§8)");

        double reqMineXp = MiningLevelManager.getRequiredXpForNextLevel(mineLvl);
        t = board.getTeam("sb_minelvl");
        if (t != null) t.setSuffix(NumberFormatter.format(new BigNumber(mineLvl), uid)
                + " §8(§7" + NumberFormatter.format(new BigNumber(mineXp), uid)
                + "§8/§7" + NumberFormatter.format(new BigNumber(reqMineXp), uid) + "§8)");
        t = board.getTeam("sb_minexp");
        if (t != null) t.setSuffix(MiningLevelManager.generateXpBar(mineXp, reqMineXp));
    }

    // Returns [totalMoneyMulti (with exponent applied), totalGemsMulti]
    private BigNumber[] calcMultipliers(PlayerProfile profile) {
        BigNumber money = new BigNumber(1.0);
        BigNumber gems  = new BigNumber(1.0);
        if (profile != null) {
            BigNumber upgrade     = UpgradeManager.getTotalMultiplier(profile.getUpgradeLevel());
            BigNumber rebirth     = RebirthManager.calculateMoneyMultiplier(profile.getRebirthPoints());
            BigNumber tier        = TierManager.getMultiplierForTier(profile.getTier());
            BigNumber mine        = MiningLevelManager.getMoneyMultiplier(profile.getMiningLevel());
            BigNumber farmMult    = new BigNumber(profile.getFarmMulti());
            BigNumber farmUpgMult = UpgradeManager.getFarmTotalMultiplier(profile.getFarmMultiUpgradeLevel());
            BigNumber grindFarm   = new BigNumber(GrindManager.getFarmMulti(profile.getGrindFarmMultiLevel()));

            BigNumber perkMult = PerkManager.getTotalPerkMultiplierBig(profile.getPerkCounts());
            BigNumber allMoney = upgrade.multiply(rebirth).multiply(tier).multiply(mine).multiply(farmMult).multiply(farmUpgMult).multiply(grindFarm).multiply(perkMult);

            // Apply money exponent (grind base + armor additive) × artifact
            double baseExp   = GrindManager.getMoneyExponent(profile.getGrindExponentLevel());
            double armorExp  = ArmorManager.getMultiplier(profile, ArmorType.EXPONENT);
            double artifactExpMult = ArtifactManager.getMultiplierDouble(profile, ArtifactType.EXPONENT);
            double exponent  = (baseExp + armorExp) * artifactExpMult;
            double log10 = Math.log10(allMoney.getMantissa()) + allMoney.getExponent();
            money = UpgradeManager.fromLog10(exponent * log10);

            BigNumber gemPickaxe  = new BigNumber(multigainer.multigainer.tools.PickaxeManager.getGemMultiplier(profile.getGemMultiLevel()));
            BigNumber gemUpgMult  = UpgradeManager.getGemTotalMultiplier(profile.getGemUpgradeLevel());
            BigNumber grindGem    = new BigNumber(GrindManager.getGemMulti(profile.getGrindGemMultiLevel()));
            BigNumber artifactGem = ArtifactManager.getMultiplier(profile, ArtifactType.GEM);
            BigNumber armorGems   = new BigNumber(ArmorManager.getMultiplier(profile, ArmorType.GEMS));
            gems = MiningLevelManager.getGemsMultiplier(profile.getMiningLevel())
                    .multiply(gemPickaxe).multiply(gemUpgMult).multiply(grindGem)
                    .multiply(artifactGem).multiply(armorGems);
        }
        return new BigNumber[]{ money, gems };
    }
}
