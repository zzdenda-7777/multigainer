package multigainer.multigainer.data;

import multigainer.multigainer.math.BigNumber;

public class PlayerProfile {
    private BigNumber money;
    private BigNumber gems;
    private BigNumber rubies;
    private int tierPoints = 0;
    private int upgradeLevel, tier, farmingLevel, miningLevel, rebirthCount;
    private double farmingXp, miningXp, rebirthPoints;

    // Pickaxe progression
    private int pickaxeTier = 0;
    private int miningSpeedLevel = 0;
    private int xpMultiLevel = 0;
    private int gemMultiLevel = 0;
    private long[] blockStorage = new long[17];

    // Farming progression
    private long[] seedStorage = new long[7];       // 7 compressed seed tiers
    private double farmMulti = 1.0;                 // starts at 1, +0.001 per crop
    private int chosenCrop = 0;                     // index into FarmingManager.CROP_NAMES
    private boolean[] enchantMessagesEnabled = { true, true, true, true };
    private int hoeTier = 0;
    private boolean autoMerge = false;              // auto-compress seeds on harvest (off by default)

    public PlayerProfile() {
        this(new BigNumber(0), new BigNumber(0), new BigNumber(0), 0, 1, 0, 1, 0.0, 1, 0.0, 0.0, 0);
    }

    public PlayerProfile(BigNumber money, BigNumber gems, BigNumber rubies, int upgradeLevel, int tier, int tierPoints,
                         int farmingLevel, double farmingXp, int miningLevel, double miningXp,
                         double rebirthPoints, int rebirthCount) {
        this.money = money; this.gems = gems; this.rubies = rubies;
        this.upgradeLevel = upgradeLevel; this.tier = tier; this.tierPoints = tierPoints;
        this.farmingLevel = farmingLevel; this.farmingXp = farmingXp;
        this.miningLevel = miningLevel; this.miningXp = miningXp;
        this.rebirthPoints = rebirthPoints; this.rebirthCount = rebirthCount;
    }

    // ── Currency ─────────────────────────────────────────────────
    public BigNumber getMoney() { return money; }
    public void setMoney(BigNumber money) { this.money = money; }
    public void addMoney(BigNumber amount) { this.money = this.money.add(amount); }

    public BigNumber getGems() { return gems; }
    public void setGems(BigNumber gems) { this.gems = gems; }
    public void addGems(BigNumber amount) { this.gems = this.gems.add(amount); }

    public BigNumber getRubies() { return rubies; }
    public void setRubies(BigNumber rubies) { this.rubies = rubies; }
    public void addRubies(BigNumber amount) { this.rubies = this.rubies.add(amount); }

    // ── Core stats ────────────────────────────────────────────────
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

    public int getTierPoints() { return this.tierPoints; }
    public void setTierPoints(int tierPoints) { this.tierPoints = tierPoints; }

    // ── Pickaxe ───────────────────────────────────────────────────
    public int getPickaxeTier() { return pickaxeTier; }
    public void setPickaxeTier(int pickaxeTier) { this.pickaxeTier = Math.max(0, Math.min(6, pickaxeTier)); }

    public int getMiningSpeedLevel() { return miningSpeedLevel; }
    public void setMiningSpeedLevel(int level) { this.miningSpeedLevel = level; }

    public int getXpMultiLevel() { return xpMultiLevel; }
    public void setXpMultiLevel(int level) { this.xpMultiLevel = level; }

    public int getGemMultiLevel() { return gemMultiLevel; }
    public void setGemMultiLevel(int level) { this.gemMultiLevel = level; }

    public long getBlockStorage(int index) {
        if (index < 0 || index >= blockStorage.length) return 0;
        return blockStorage[index];
    }
    public void setBlockStorage(int index, long value) {
        if (index >= 0 && index < blockStorage.length) blockStorage[index] = value;
    }
    public void incrementBlockStorage(int index) {
        if (index >= 0 && index < blockStorage.length) blockStorage[index]++;
    }
    public long[] getBlockStorageArray() { return blockStorage; }
    public void setBlockStorageArray(long[] storage) { this.blockStorage = storage; }

    // ── Farming (seed currencies) ─────────────────────────────────
    public long getSeedStorage(int tier) {
        if (tier < 0 || tier >= seedStorage.length) return 0;
        return seedStorage[tier];
    }
    public void setSeedStorage(int tier, long value) {
        if (tier >= 0 && tier < seedStorage.length) seedStorage[tier] = Math.max(0, value);
    }
    public void addSeeds(long amount) {
        if (amount > 0) seedStorage[0] += amount;
    }
    public long[] getSeedStorageArray() { return seedStorage; }
    public void setSeedStorageArray(long[] arr) { this.seedStorage = arr; }

    public double getFarmMulti() { return farmMulti; }
    public void setFarmMulti(double farmMulti) { this.farmMulti = Math.max(1.0, farmMulti); }
    public void incrementFarmMulti() { this.farmMulti += 0.001; }

    public int getChosenCrop() { return chosenCrop; }
    public void setChosenCrop(int crop) { this.chosenCrop = Math.max(0, Math.min(15, crop)); }

    public boolean isEnchantMessageEnabled(int enchantIndex) {
        if (enchantIndex < 0 || enchantIndex >= enchantMessagesEnabled.length) return true;
        return enchantMessagesEnabled[enchantIndex];
    }
    public void setEnchantMessageEnabled(int enchantIndex, boolean enabled) {
        if (enchantIndex >= 0 && enchantIndex < enchantMessagesEnabled.length)
            enchantMessagesEnabled[enchantIndex] = enabled;
    }
    public boolean[] getEnchantMessagesEnabled() { return enchantMessagesEnabled; }
    public void setEnchantMessagesEnabled(boolean[] arr) { this.enchantMessagesEnabled = arr; }

    public int getHoeTier() { return hoeTier; }
    public void setHoeTier(int tier) { this.hoeTier = Math.max(0, Math.min(5, tier)); }

    public boolean isAutoMerge() { return autoMerge; }
    public void setAutoMerge(boolean autoMerge) { this.autoMerge = autoMerge; }
}
