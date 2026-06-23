package multigainer.multigainer.data;

import multigainer.multigainer.math.BigNumber;

public class PlayerProfile {
    private BigNumber money;
    private BigNumber gems;
    private BigNumber rubies;
    private int tierPoints = 0;
    private int upgradeLevel, tier, farmingLevel, miningLevel, rebirthCount;
    private double farmingXp, miningXp, rebirthPoints;

    // Default constructor
    public PlayerProfile() {
        this(new BigNumber(0), new BigNumber(0), new BigNumber(0), 0, 1, 1, 0.0, 1, 0.0, 0.0, 0);
    }

    // Full constructor
    public PlayerProfile(BigNumber money, BigNumber gems, BigNumber rubies, int upgradeLevel, int tier,
                         int farmingLevel, double farmingXp, int miningLevel, double miningXp,
                         double rebirthPoints, int rebirthCount) {
        this.money = money; this.gems = gems; this.rubies = rubies;
        this.upgradeLevel = upgradeLevel; this.tier = tier;
        this.farmingLevel = farmingLevel; this.farmingXp = farmingXp;
        this.miningLevel = miningLevel; this.miningXp = miningXp;
        this.rebirthPoints = rebirthPoints; this.rebirthCount = rebirthCount;
    }

    // --- Currency Methods ---
    public BigNumber getMoney() { return money; }
    public void setMoney(BigNumber money) { this.money = money; }
    public void addMoney(BigNumber amount) { this.money = this.money.add(amount); }

    public BigNumber getGems() { return gems; }
    public void setGems(BigNumber gems) { this.gems = gems; }
    public void addGems(BigNumber amount) { this.gems = this.gems.add(amount); }

    public BigNumber getRubies() { return rubies; }
    public void setRubies(BigNumber rubies) { this.rubies = rubies; }
    public void addRubies(BigNumber amount) { this.rubies = this.rubies.add(amount); }

    // --- Stats Methods ---
    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int level) { this.upgradeLevel = level; }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public int getFarmingLevel() { return farmingLevel; }
    public void setFarmingLevel(int level) { this.farmingLevel = level; }
    public double getFarmingXp() { return farmingXp; }
    public void setFarmingXp(double xp) { this.farmingXp = xp; }

    public int getMiningLevel() { return miningLevel; }
    public void setMiningLevel(int level) { this.miningLevel = level; }
    public double getMiningXp() { return miningXp; }
    public void setMiningXp(double xp) { this.miningXp = xp; }

    public double getRebirthPoints() { return rebirthPoints; }
    public void setRebirthPoints(double points) { this.rebirthPoints = points; }
    public int getRebirthCount() { return rebirthCount; }
    public void setRebirthCount(int count) { this.rebirthCount = count; }

    // --- Added Tier Points Methods ---
    public int getTierPoints() {
        return this.tierPoints;
    }

    public void setTierPoints(int tierPoints) {
        this.tierPoints = tierPoints;
    }
}