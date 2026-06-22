package multigainer.multigainer.data;

import multigainer.multigainer.math.BigNumber;

public class PlayerProfile {
    private BigNumber money;
    private BigNumber gems;
    private BigNumber rubies;
    private int upgradeLevel;

    // Main constructor using the custom BigNumber ecosystem
    public PlayerProfile(BigNumber money, BigNumber gems, BigNumber rubies, int upgradeLevel) {
        this.money = money;
        this.gems = gems;
        this.rubies = rubies;
        this.upgradeLevel = upgradeLevel;
    }

    // Overloaded file-loading constructor to bridge raw values from YAML configuration configurations safely
    public PlayerProfile(double money, int gems, int rubies) {
        this.money = new BigNumber(money);
        this.gems = new BigNumber((double) gems);
        this.rubies = new BigNumber((double) rubies);
        this.upgradeLevel = 0; // Starts at baseline level 0 when converting from flat numerical parameters
    }

    // Convenient overloaded constructor for setting up brand new players easily
    public PlayerProfile() {
        this.money = new BigNumber(0);
        this.gems = new BigNumber(0);
        this.rubies = new BigNumber(0);
        this.upgradeLevel = 0;
    }

    // Money Methods
    public BigNumber getMoney() { return money; }
    public void setMoney(BigNumber money) { this.money = money; }
    public void addMoney(BigNumber amount) {
        this.money = this.money.add(amount);
    }

    // Gems Methods
    public BigNumber getGems() { return gems; }
    public void setGems(BigNumber gems) { this.gems = gems; }
    public void addGems(BigNumber amount) {
        this.gems = this.gems.add(amount);
    }

    // Rubies Methods
    public BigNumber getRubies() { return rubies; }
    public void setRubies(BigNumber rubies) { this.rubies = rubies; }
    public void addRubies(BigNumber amount) {
        this.rubies = this.rubies.add(amount);
    }

    // Upgrade Level Methods
    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }
}