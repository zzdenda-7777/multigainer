package multigainer.multigainer.data;

import multigainer.multigainer.math.BigNumber;

public class PlayerProfile {
    private BigNumber money;
    private BigNumber gems;
    private BigNumber rubies;
    private int tierPoints = 0;
    private int upgradeLevel, tier, farmingLevel, miningLevel, rebirthCount;
    private double farmingXp, miningXp;
    private BigNumber rebirthPoints;

    // Pickaxe progression
    private int pickaxeTier = 0;
    private int miningSpeedLevel = 0;
    private int xpMultiLevel = 0;
    private int gemMultiLevel = 0;
    private long[] blockStorage = new long[17];

    // Farming progression
    private long[] seedStorage = new long[7];
    private double farmMulti = 1.0;
    private int chosenCrop = 0;
    private boolean[] enchantMessagesEnabled = { true, true, true, true };
    private int hoeTier = 0;
    private boolean autoMerge = false;

    // Upgrade levels (reset on tier and rebirth)
    private int gemUpgradeLevel = 0;
    private int farmMultiUpgradeLevel = 0;

    // Item hotbar slot positions (persisted so players can rearrange freely)
    private int hoeSlot     = 0;
    private int pickaxeSlot = 1;
    private int upgradeSlot = 4;

    // Artifact slots (artifact ID string, empty = none)
    private String[] artifactSlots          = {"", "", ""};
    private boolean  artifactSlot1Unlocked  = false;
    private boolean  artifactSlot2Unlocked  = false;

    // Artifact vault (45-slot personal storage)
    private String[] artifactVault = new String[45];

    // ── Production / Worker ───────────────────────────────────────────────────
    private int    workerLevel  = 0;
    private double workerXp     = 0.0;
    private double workerEnergy = 0.0;

    // ── Farming messages ──────────────────────────────────────────────────────
    private boolean levelUpFarmMessageEnabled = true;

    // ── Grinding Points ───────────────────────────────────────────────────────
    private double grindingPoints = 0.0;
    private boolean grindMessagesEnabled = true;

    // Grind upgrade levels (permanent, never reset)
    private int grindChanceLevel    = 0;  // reduces GP drop denominator by 1% compound
    private int grindExponentLevel  = 0;  // +0.01 to money exponent per level
    private int grindFarmMultiLevel = 0;  // 1.15x farm multi per level (money income)
    private int grindGemMultiLevel  = 0;  // 1.05x gem multi per level
    private int grindFarmXpLevel    = 0;  // 1.075x farm XP per level
    private int grindMineXpLevel    = 0;  // 1.04x mine XP per level
    private int grindSeedMultiLevel = 0;  // 1.03x seed gain per level
    private int grindGPMultiLevel   = 0;  // 1.25x grinding points earned per level

    public PlayerProfile() {
        this(new BigNumber(0), new BigNumber(0), new BigNumber(0), 0, 1, 0, 1, 0.0, 1, 0.0, new BigNumber(0), 0);
    }

    public PlayerProfile(BigNumber money, BigNumber gems, BigNumber rubies, int upgradeLevel, int tier, int tierPoints,
                         int farmingLevel, double farmingXp, int miningLevel, double miningXp,
                         BigNumber rebirthPoints, int rebirthCount) {
        this.money = money; this.gems = gems; this.rubies = rubies;
        this.upgradeLevel = upgradeLevel; this.tier = tier; this.tierPoints = tierPoints;
        this.farmingLevel = farmingLevel; this.farmingXp = farmingXp;
        this.miningLevel = miningLevel; this.miningXp = miningXp;
        this.rebirthPoints = rebirthPoints; this.rebirthCount = rebirthCount;
    }

    // ── Currency ──────────────────────────────────────────────────────────────
    public BigNumber getMoney() { return money; }
    public void setMoney(BigNumber money) { this.money = money; }
    public void addMoney(BigNumber amount) { this.money = this.money.add(amount); }

    public BigNumber getGems() { return gems; }
    public void setGems(BigNumber gems) { this.gems = gems; }
    public void addGems(BigNumber amount) { this.gems = this.gems.add(amount); }

    public BigNumber getRubies() { return rubies; }
    public void setRubies(BigNumber rubies) { this.rubies = rubies; }
    public void addRubies(BigNumber amount) { this.rubies = this.rubies.add(amount); }

    // ── Grinding Points ───────────────────────────────────────────────────────
    public double getGrindingPoints() { return grindingPoints; }
    public void setGrindingPoints(double gp) { this.grindingPoints = Math.max(0.0, gp); }
    public void addGrindingPoints(double amount) { this.grindingPoints += amount; }

    public boolean isGrindMessagesEnabled() { return grindMessagesEnabled; }
    public void setGrindMessagesEnabled(boolean enabled) { this.grindMessagesEnabled = enabled; }

    public int getGrindChanceLevel()    { return grindChanceLevel; }
    public void setGrindChanceLevel(int l)    { this.grindChanceLevel    = Math.max(0, l); }

    public int getGrindExponentLevel()  { return grindExponentLevel; }
    public void setGrindExponentLevel(int l)  { this.grindExponentLevel  = Math.max(0, l); }

    public int getGrindFarmMultiLevel() { return grindFarmMultiLevel; }
    public void setGrindFarmMultiLevel(int l) { this.grindFarmMultiLevel = Math.max(0, l); }

    public int getGrindGemMultiLevel()  { return grindGemMultiLevel; }
    public void setGrindGemMultiLevel(int l)  { this.grindGemMultiLevel  = Math.max(0, l); }

    public int getGrindFarmXpLevel()    { return grindFarmXpLevel; }
    public void setGrindFarmXpLevel(int l)    { this.grindFarmXpLevel    = Math.max(0, l); }

    public int getGrindMineXpLevel()    { return grindMineXpLevel; }
    public void setGrindMineXpLevel(int l)    { this.grindMineXpLevel    = Math.max(0, l); }

    public int getGrindSeedMultiLevel() { return grindSeedMultiLevel; }
    public void setGrindSeedMultiLevel(int l) { this.grindSeedMultiLevel = Math.max(0, l); }

    public int getGrindGPMultiLevel()   { return grindGPMultiLevel; }
    public void setGrindGPMultiLevel(int l)   { this.grindGPMultiLevel   = Math.max(0, l); }

    // ── Farming level-up message ──────────────────────────────────────────────
    public boolean isLevelUpFarmMessageEnabled() { return levelUpFarmMessageEnabled; }
    public void setLevelUpFarmMessageEnabled(boolean enabled) { this.levelUpFarmMessageEnabled = enabled; }

    // ── Perks ─────────────────────────────────────────────────────────────────
    private int[] perkCounts       = new int[5];
    private int[] perkChanceLevels = new int[5];
    private boolean[] perkMessagesEnabled = {true, true, true, true, true};

    public int getPerkCount(int i) { return (i >= 0 && i < 5) ? perkCounts[i] : 0; }
    public void setPerkCount(int i, int count) { if (i >= 0 && i < 5) perkCounts[i] = Math.max(0, count); }
    public void incrementPerkCount(int i) { if (i >= 0 && i < 5) perkCounts[i]++; }
    public int[] getPerkCounts() { return perkCounts; }
    public void setPerkCounts(int[] counts) { this.perkCounts = counts; }

    public int getPerkChanceLevel(int i) { return (i >= 0 && i < 5) ? perkChanceLevels[i] : 0; }
    public void setPerkChanceLevel(int i, int level) { if (i >= 0 && i < 5) perkChanceLevels[i] = Math.max(0, level); }
    public int[] getPerkChanceLevels() { return perkChanceLevels; }
    public void setPerkChanceLevels(int[] levels) { this.perkChanceLevels = levels; }

    public boolean isPerkMessageEnabled(int i) { return (i >= 0 && i < 5) && perkMessagesEnabled[i]; }
    public void setPerkMessageEnabled(int i, boolean enabled) { if (i >= 0 && i < 5) perkMessagesEnabled[i] = enabled; }
    public boolean[] getPerkMessagesEnabled() { return perkMessagesEnabled; }
    public void setPerkMessagesEnabled(boolean[] enabled) { this.perkMessagesEnabled = enabled; }

    // ── Core stats ────────────────────────────────────────────────────────────
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

    public BigNumber getRebirthPoints() { return rebirthPoints; }
    public void setRebirthPoints(BigNumber points) { this.rebirthPoints = points != null ? points : new BigNumber(0); }
    public int getRebirthCount() { return rebirthCount; }
    public void setRebirthCount(int count) { this.rebirthCount = count; }

    public int getTierPoints() { return this.tierPoints; }
    public void setTierPoints(int tierPoints) { this.tierPoints = tierPoints; }

    // ── Pickaxe ───────────────────────────────────────────────────────────────
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

    // ── Farming ───────────────────────────────────────────────────────────────
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

    // ── Upgrade levels ────────────────────────────────────────────────────────
    public int getGemUpgradeLevel() { return gemUpgradeLevel; }
    public void setGemUpgradeLevel(int level) { this.gemUpgradeLevel = Math.max(0, level); }
    public int getFarmMultiUpgradeLevel() { return farmMultiUpgradeLevel; }
    public void setFarmMultiUpgradeLevel(int level) { this.farmMultiUpgradeLevel = Math.max(0, level); }

    public int getHoeSlot()          { return hoeSlot; }
    public void setHoeSlot(int s)    { this.hoeSlot = Math.max(0, Math.min(35, s)); }
    public int getPickaxeSlot()      { return pickaxeSlot; }
    public void setPickaxeSlot(int s){ this.pickaxeSlot = Math.max(0, Math.min(35, s)); }
    public int getUpgradeSlot()      { return upgradeSlot; }
    public void setUpgradeSlot(int s){ this.upgradeSlot = Math.max(0, Math.min(35, s)); }

    // Artifact slots
    public String getArtifactSlot(int i)          { return (i >= 0 && i < 3) ? artifactSlots[i] : ""; }
    public void   setArtifactSlot(int i, String v) { if (i >= 0 && i < 3) artifactSlots[i] = v != null ? v : ""; }
    public boolean isArtifactSlotUnlocked(int i) {
        if (i == 0) return true;
        if (i == 1) return artifactSlot1Unlocked;
        if (i == 2) return artifactSlot2Unlocked;
        return false;
    }
    public void setArtifactSlotUnlocked(int i, boolean v) {
        if (i == 1) artifactSlot1Unlocked = v;
        else if (i == 2) artifactSlot2Unlocked = v;
    }

    public void addBlockStorage(int index, long amount) {
        if (index >= 0 && index < blockStorage.length)
            blockStorage[index] = Math.max(0, blockStorage[index] + amount);
    }

    public int    getWorkerLevel()          { return workerLevel; }
    public void   setWorkerLevel(int v)     { this.workerLevel = Math.max(0, v); }
    public double getWorkerXp()             { return workerXp; }
    public void   setWorkerXp(double v)     { this.workerXp = Math.max(0, v); }
    public double getWorkerEnergy()         { return workerEnergy; }
    public void   setWorkerEnergy(double v) { this.workerEnergy = Math.max(0, v); }
    public void   addWorkerEnergy(double v) { this.workerEnergy += v; }

    // ── Armor system ──────────────────────────────────────────────────────────
    // pieceIndex: 0=helmet, 1=chest, 2=legs, 3=boots
    // helmet unlocks automatically via tier; others are purchased
    private boolean[] armorPieceUnlocked = {false, false, false, false};
    private int[]    armorPieceType      = {-1, -1, -1, -1}; // ArmorType ordinal, -1 = none
    private double[] armorPieceValue     = {0.0, 0.0, 0.0, 0.0};
    private int      armorLowBuys        = 0;
    private int      armorMedBuys        = 0;
    private int      armorHighBuys       = 0;
    private boolean  skipAnimationUnlocked = false;
    private boolean  skipAnimationEnabled  = false;

    // ── Farming statistics ────────────────────────────────────────────────────
    private long cropsFarmed = 0L;

    public boolean isArmorPieceUnlocked(int i)      { return (i >= 0 && i < 4) && armorPieceUnlocked[i]; }
    public void    setArmorPieceUnlocked(int i, boolean v) { if (i >= 0 && i < 4) armorPieceUnlocked[i] = v; }
    public int     getArmorType(int i)               { return (i >= 0 && i < 4) ? armorPieceType[i] : -1; }
    public void    setArmorType(int i, int type)     { if (i >= 0 && i < 4) armorPieceType[i] = type; }
    public double  getArmorValue(int i)              { return (i >= 0 && i < 4) ? armorPieceValue[i] : 0.0; }
    public void    setArmorValue(int i, double v)    { if (i >= 0 && i < 4) armorPieceValue[i] = v; }
    public int     getArmorLowBuys()                 { return armorLowBuys; }
    public void    setArmorLowBuys(int v)            { this.armorLowBuys  = Math.max(0, v); }
    public int     getArmorMedBuys()                 { return armorMedBuys; }
    public void    setArmorMedBuys(int v)            { this.armorMedBuys  = Math.max(0, v); }
    public int     getArmorHighBuys()                { return armorHighBuys; }
    public void    setArmorHighBuys(int v)           { this.armorHighBuys = Math.max(0, v); }
    public boolean isSkipAnimationUnlocked()         { return skipAnimationUnlocked; }
    public void    setSkipAnimationUnlocked(boolean v) { this.skipAnimationUnlocked = v; }
    public boolean isSkipAnimationEnabled()          { return skipAnimationEnabled; }
    public void    setSkipAnimationEnabled(boolean v)  { this.skipAnimationEnabled = v; }
    public long    getCropsFarmed()                  { return cropsFarmed; }
    public void    setCropsFarmed(long v)            { this.cropsFarmed = Math.max(0, v); }
    public void    incrementCropsFarmed()            { this.cropsFarmed++; }

    public String getArtifactVaultSlot(int i) {
        if (i < 0 || i >= artifactVault.length) return "";
        return artifactVault[i] == null ? "" : artifactVault[i];
    }
    public void setArtifactVaultSlot(int i, String v) {
        if (i >= 0 && i < artifactVault.length) artifactVault[i] = v == null ? "" : v;
    }
    public String[] getArtifactVault() { return artifactVault; }
    public void setArtifactVault(String[] v) { this.artifactVault = v != null ? v : new String[45]; }
}
