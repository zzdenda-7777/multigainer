package multigainer.multigainer.data;

import multigainer.multigainer.math.BigNumber;

public class PlayerProfile {
    private BigNumber money;
    private BigNumber gems;
    private BigNumber rubies;
    private int upgradeLevel;

    // Farming tracking fields
    private int farmingLevel;
    private double farmingXp;

    // INTEGRATED: Mining tracking fields
    private int miningLevel;
    private double miningXp;

    // NEW: Rebirth tracking fields
    private double rebirthPoints;
    private int rebirthCount;

    // Main constructor
    public PlayerProfile(BigNumber money, BigNumber gems, BigNumber rubies, int upgradeLevel) {
        this.money = money;
        this.gems = gems;
        this.rubies = rubies;
        this.upgradeLevel = upgradeLevel;

        this.farmingLevel = 1;
        this.farmingXp = 0.0;
        this.miningLevel = 1;
        this.miningXp = 0.0;

        // Initialize rebirth defaults
        this.rebirthPoints = 0.0;
        this.rebirthCount = 0;
    }

    // Overloaded file-loading constructor
    public PlayerProfile(double money, int gems, int rubies) {
        this.money = new BigNumber(money);
        this.gems = new BigNumber((double) gems);
        this.rubies = new BigNumber((double) rubies);
        this.upgradeLevel = 0;

        this.farmingLevel = 1;
        this.farmingXp = 0.0;
        this.miningLevel = 1;
        this.miningXp = 0.0;

        // Initialize rebirth defaults
        this.rebirthPoints = 0.0;
        this.rebirthCount = 0;
    }

    // Convenient constructor for brand new players
    public PlayerProfile() {
        this.money = new BigNumber(0);
        this.gems = new BigNumber(0);
        this.rubies = new BigNumber(0);
        this.upgradeLevel = 0;

        this.farmingLevel = 1;
        this.farmingXp = 0.0;
        this.miningLevel = 1;
        this.miningXp = 0.0;

        // Initialize rebirth defaults
        this.rebirthPoints = 0.0;
        this.rebirthCount = 0;
    }

    // --- Existing Methods ---
    public BigNumber getMoney() { return money; }
    public void setMoney(BigNumber money) { this.money = money; }
    public void addMoney(BigNumber amount) { this.money = this.money.add(amount); }

    public BigNumber getGems() { return gems; }
    public void setGems(BigNumber gems) { this.gems = gems; }
    public void addGems(BigNumber amount) { this.gems = this.gems.add(amount); }

    public BigNumber getRubies() { return rubies; }
    public void setRubies(BigNumber rubies) { this.rubies = rubies; }
    public void addRubies(BigNumber amount) { this.rubies = this.rubies.add(amount); }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }

    public int getFarmingLevel() { return farmingLevel; }
    public void setFarmingLevel(int farmingLevel) { this.farmingLevel = farmingLevel; }
    public double getFarmingXp() { return farmingXp; }
    public void setFarmingXp(double farmingXp) { this.farmingXp = farmingXp; }

    public int getMiningLevel() { return miningLevel; }
    public void setMiningLevel(int miningLevel) { this.miningLevel = miningLevel; }
    public double getMiningXp() { return miningXp; }
    public void setMiningXp(double miningXp) { this.miningXp = miningXp; }

    // --- NEW: Rebirth System Methods ---
    public double getRebirthPoints() { return rebirthPoints; }
    public void setRebirthPoints(double points) { this.rebirthPoints = points; }

    public int getRebirthCount() { return rebirthCount; }
    public void setRebirthCount(int count) { this.rebirthCount = count; }
}