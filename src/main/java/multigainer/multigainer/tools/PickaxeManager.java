package multigainer.multigainer.tools;

import multigainer.multigainer.math.BigNumber;
import org.bukkit.Material;

public class PickaxeManager {

    // All mineable blocks in order (index = tier unlock order)
    public static final Material[] BLOCKS = {
        Material.COBBLESTONE,
        Material.COBBLED_DEEPSLATE,
        Material.COPPER_ORE,
        Material.DEEPSLATE_COPPER_ORE,
        Material.COAL_ORE,
        Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE,
        Material.DEEPSLATE_IRON_ORE,
        Material.REDSTONE_ORE,
        Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE,
        Material.DEEPSLATE_LAPIS_ORE,
        Material.GOLD_ORE,
        Material.DEEPSLATE_GOLD_ORE,
        Material.DIAMOND_ORE,
        Material.DEEPSLATE_DIAMOND_ORE,
        Material.NETHERITE_BLOCK
    };

    public static final String[] BLOCK_NAMES = {
        "Cobblestone", "Cobbled Deepslate", "Copper Ore", "Deepslate Copper Ore",
        "Coal Ore", "Deepslate Coal Ore", "Iron Ore", "Deepslate Iron Ore",
        "Redstone Ore", "Deepslate Redstone Ore", "Lapis Ore", "Deepslate Lapis Ore",
        "Gold Ore", "Deepslate Gold Ore", "Diamond Ore", "Deepslate Diamond Ore",
        "Netherite Block"
    };

    // SQLite column names for each block's storage counter
    public static final String[] BLOCK_COLUMN_NAMES = {
        "storage_cobblestone", "storage_cobbled_deepslate",
        "storage_copper_ore", "storage_deepslate_copper_ore",
        "storage_coal_ore", "storage_deepslate_coal_ore",
        "storage_iron_ore", "storage_deepslate_iron_ore",
        "storage_redstone_ore", "storage_deepslate_redstone_ore",
        "storage_lapis_ore", "storage_deepslate_lapis_ore",
        "storage_gold_ore", "storage_deepslate_gold_ore",
        "storage_diamond_ore", "storage_deepslate_diamond_ore",
        "storage_netherite_block"
    };

    public static final String[] TIER_NAMES = {
        "Wooden", "Stone", "Copper", "Iron", "Diamond", "Netherite", "Golden"
    };

    // Chat color codes per tier
    public static final String[] TIER_COLORS = {
        "§f", "§7", "§6", "§7", "§b", "§d", "§e"
    };

    // Physical item material per tier (copper has no vanilla pickaxe, use golden)
    public static final Material[] TIER_MATERIALS = {
        Material.WOODEN_PICKAXE,
        Material.STONE_PICKAXE,
        Material.GOLDEN_PICKAXE,    // copper tier — closest vanilla look
        Material.IRON_PICKAXE,
        Material.DIAMOND_PICKAXE,
        Material.NETHERITE_PICKAXE,
        Material.GOLDEN_PICKAXE     // golden
    };

    // Highest BLOCKS index (inclusive) that each tier can mine
    // Wooden=1, Stone=4, Copper=7, Iron=9, Diamond=12, Netherite=14, Golden=17
    public static final int[] MAX_BLOCK_INDEX = {0, 3, 6, 8, 11, 13, 16};

    // [requiredTier, requiredMiningLevel] to upgrade FROM this tier index to the next
    public static final int[][] TIER_UP_REQUIREMENTS = {
        {1,   15},       // Wooden (0) -> Stone (1)
        {5,   150},      // Stone  (1) -> Copper (2)
        {15,  1500},     // Copper (2) -> Iron (3)
        {45,  10000},    // Iron   (3) -> Diamond (4)
        {100, 50000},    // Diamond(4) -> Netherite (5)
        {200, 100000}    // Netherite(5)-> Golden (6)
    };

    public static int getBlockIndex(Material material) {
        for (int i = 0; i < BLOCKS.length; i++) {
            if (BLOCKS[i] == material) return i;
        }
        return -1;
    }

    // Minimum pickaxe tier required to mine a block at the given index
    public static int getMinTierForBlock(int blockIndex) {
        for (int tier = 0; tier < MAX_BLOCK_INDEX.length; tier++) {
            if (blockIndex <= MAX_BLOCK_INDEX[tier]) return tier;
        }
        return TIER_NAMES.length - 1;
    }

    // --- Upgrade cost formulas ---

    public static BigNumber getMiningSpeedCost(int currentLevel) {
        return new BigNumber(100.0 * Math.pow(7.5, currentLevel));
    }

    public static BigNumber getXpMultiCost(int currentLevel) {
        return new BigNumber(500.0 * Math.pow(1.5, currentLevel));
    }

    public static BigNumber getGemMultiCost(int currentLevel) {
        return new BigNumber(1000.0 * Math.pow(1.75, currentLevel));
    }

    // --- Multiplier values ---

    public static double getXpMultiplier(int xpMultiLevel) {
        return Math.pow(1.05, xpMultiLevel);
    }

    public static double getGemMultiplier(int gemMultiLevel) {
        return Math.pow(1.03, gemMultiLevel);
    }
}