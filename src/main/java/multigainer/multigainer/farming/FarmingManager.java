package multigainer.multigainer.farming;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FarmingManager {

    public static final int CROP_COUNT      = 16;
    public static final int SEED_TIER_COUNT = 7;

    // ── Farm field corners ────────────────────────────────────────────────────
    public static final int FARM_MIN_X = -15;
    public static final int FARM_MAX_X = 60;
    public static final int FARM_MIN_Z = -3;
    public static final int FARM_MAX_Z = 29;
    public static final int FARM_Y     = -10;

    // ── Crop names ────────────────────────────────────────────────────────────
    public static final String[] CROP_NAMES = {
        "§e§lWheat",          "§6§lPotato",         "§a§lCarrot",          "§c§lBeetroot",
        "§3§lTube Coral",     "§d§lBrain Coral",     "§9§lBubble Coral",
        "§4§lFire Coral",     "§e§lHorn Coral",
        "§3§lTube Coral Fan", "§d§lBrain Coral Fan", "§9§lBubble Coral Fan",
        "§4§lFire Coral Fan", "§e§lHorn Coral Fan",
        "§2§lLarge Fern",     "§a§lSugar Cane"
    };

    // Fake block materials for sendBlockChange (client-side only — no real blocks placed).
    // Aquatic crops use waterlogged=true in getCropBlockData() so they look colorful/alive.
    // No actual water is placed in the world.
    public static final Material[] CROP_BLOCK_MATERIALS = {
        Material.WHEAT,            // 0  Wheat
        Material.POTATOES,         // 1  Potato
        Material.CARROTS,          // 2  Carrot
        Material.BEETROOTS,        // 3  Beetroot
        Material.TUBE_CORAL_FAN,   // 4  Tube Coral
        Material.BRAIN_CORAL_FAN,  // 5  Brain Coral
        Material.BUBBLE_CORAL_FAN, // 6  Bubble Coral
        Material.FIRE_CORAL_FAN,   // 7  Fire Coral
        Material.HORN_CORAL_FAN,   // 8  Horn Coral
        Material.TUBE_CORAL_FAN,   // 9
        Material.BRAIN_CORAL_FAN,  // 10
        Material.BUBBLE_CORAL_FAN, // 11
        Material.FIRE_CORAL_FAN,   // 12
        Material.HORN_CORAL_FAN,   // 13
        Material.LARGE_FERN,          // 14 Kelp — tall green plant, no water required
        Material.SUGAR_CANE           // 15 Sugar Cane — cannot grow (fake block, client-side only)
    };

    // ── GUI inventory icons (shown in inventory, need not avoid water) ────────
    public static final Material[] CROP_DISPLAY_ITEMS = {
        Material.WHEAT,          Material.POTATO,          Material.CARROT,         Material.BEETROOT,
        Material.TUBE_CORAL,     Material.BRAIN_CORAL,     Material.BUBBLE_CORAL,
        Material.FIRE_CORAL,     Material.HORN_CORAL,
        Material.TUBE_CORAL_FAN, Material.BRAIN_CORAL_FAN, Material.BUBBLE_CORAL_FAN,
        Material.FIRE_CORAL_FAN, Material.HORN_CORAL_FAN,
        Material.KELP,           Material.SUGAR_CANE
    };

    // ── Unlock tier per crop ──────────────────────────────────────────────────
    public static final int[] CROP_UNLOCK_TIERS = {
        0, 1, 3, 6, 10, 15, 20, 30, 40, 50, 70, 90, 110, 130, 150, 200
    };

    // ── Seeds per harvest (whole numbers, ends in 0 or 5) ────────────────────
    public static final long[] CROP_SEED_MULTIPLIERS = {
        1L, 2L, 3L, 5L, 10L, 20L, 45L, 100L, 200L, 450L,
        1_000L, 2_500L, 5_000L, 15_000L, 50_000L, 150_000L
    };

    // ── Hoe tiers ─────────────────────────────────────────────────────────────
    public static final Material[] HOE_MATERIALS = {
        Material.WOODEN_HOE, Material.STONE_HOE, Material.GOLDEN_HOE,
        Material.IRON_HOE,   Material.DIAMOND_HOE, Material.NETHERITE_HOE
    };
    public static final String[] HOE_TIER_NAMES  = { "Wooden", "Stone", "Golden", "Iron", "Diamond", "Netherite" };
    public static final String[] HOE_TIER_COLORS = { "§f",     "§7",    "§e",    "§7",   "§b",      "§5"       };
    public static final int[]    HOE_TIER_REQUIREMENTS = { 0, 100, 500, 3_500, 50_000, 1_000_000 };

    // ── Seed storage tiers ────────────────────────────────────────────────────
    public static final String[] SEED_TIER_NAMES = {
        "Seeds", "Wheat", "Hay Bale", "Sand", "Red Sand", "Glass", "Black Glazed Terracotta"
    };
    public static final Material[] SEED_TIER_MATERIALS = {
        Material.WHEAT_SEEDS, Material.WHEAT, Material.HAY_BLOCK,
        Material.SAND, Material.RED_SAND, Material.GLASS, Material.BLACK_GLAZED_TERRACOTTA
    };
    public static final long COMPRESS_RATIO = 64L;

    // ── Enchant constants ─────────────────────────────────────────────────────
    public static final String[] ENCHANT_NAMES = { "TNT", "Nuke", "World Eater", "Universe Destroyer" };
    private static final double[] ENCHANT_BASE_CHANCE = { 0.01, 0.001, 0.00005, 0.000001 };
    public static final long[] ENCHANT_SEED_MULTI = { 10L, 250L, 7_500L, 100_000L };

    // ── Core methods ──────────────────────────────────────────────────────────

    public static long getSeedMultiplier(int cropIndex) {
        if (cropIndex < 0 || cropIndex >= CROP_SEED_MULTIPLIERS.length) return 1L;
        return CROP_SEED_MULTIPLIERS[cropIndex];
    }

    public static double getEnchantChance(int enchantIndex, int farmLevel) {
        return Math.min(farmLevel * ENCHANT_BASE_CHANCE[enchantIndex], 25.0);
    }

    public static int getHoeTierForFarmLevel(int farmLevel) {
        for (int i = HOE_TIER_REQUIREMENTS.length - 1; i >= 0; i--) {
            if (farmLevel >= HOE_TIER_REQUIREMENTS[i]) return i;
        }
        return 0;
    }

    // Build block data for fake crop display; always non-waterlogged to prevent water visuals.
    public static BlockData getCropBlockData(int cropIndex) {
        if (cropIndex < 0 || cropIndex >= CROP_BLOCK_MATERIALS.length) cropIndex = 0;
        BlockData data = CROP_BLOCK_MATERIALS[cropIndex].createBlockData();
        if (data instanceof Ageable ageable) ageable.setAge(ageable.getMaximumAge());
        // Explicitly non-waterlogged — prevents any water visual on the client.
        if (data instanceof Waterlogged wl) wl.setWaterlogged(false);
        return data;
    }

    // Send fake crop blocks across the entire field, batched for lag-free delivery.
    // For every crop:
    //   y-1 (FARM_Y-1): fake FARMLAND so the crop always appears to sit on soil
    //   y   (FARM_Y):   the crop visual itself
    //   y+1 (FARM_Y+1): AIR for aquatic crops to prevent any overhead water from showing
    // Fake blocks are PER-PLAYER (sendBlockChange is client-side only — other players are unaffected).
    public static void sendFieldCropChange(Player player, int cropIndex, Multigainer plugin) {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return;

        boolean isAquatic    = cropIndex >= 4;
        BlockData cropData   = getCropBlockData(cropIndex);
        BlockData farmData   = Material.FARMLAND.createBlockData();
        BlockData airData    = Material.AIR.createBlockData();

        int totalX      = FARM_MAX_X - FARM_MIN_X + 1;
        int totalZ      = FARM_MAX_Z - FARM_MIN_Z + 1;
        int totalBlocks = totalX * totalZ;
        final int BATCH = 500;

        for (int b = 0; b * BATCH < totalBlocks; b++) {
            final int start = b * BATCH;
            final int end   = Math.min(start + BATCH, totalBlocks);
            new BukkitRunnable() {
                @Override public void run() {
                    if (!player.isOnline()) return;
                    for (int i = start; i < end; i++) {
                        int x = FARM_MIN_X + (i / totalZ);
                        int z = FARM_MIN_Z + (i % totalZ);
                        // Always show farmland beneath the crop
                        player.sendBlockChange(new Location(world, x, FARM_Y - 1, z), farmData);
                        // Send the crop visual
                        player.sendBlockChange(new Location(world, x, FARM_Y, z), cropData);
                        // For aquatic crops clear the block above to prevent overhead water
                        if (isAquatic) {
                            player.sendBlockChange(new Location(world, x, FARM_Y + 1, z), airData);
                        }
                    }
                }
            }.runTaskLater(plugin, (long) b);
        }
    }

    // Auto-merge all seed tiers bottom-up
    public static void runAutoMerge(PlayerProfile profile) {
        for (int tier = 0; tier < SEED_TIER_COUNT - 1; tier++) {
            long count     = profile.getSeedStorage(tier);
            long converted = count / COMPRESS_RATIO;
            if (converted > 0) {
                profile.setSeedStorage(tier, count % COMPRESS_RATIO);
                profile.setSeedStorage(tier + 1, profile.getSeedStorage(tier + 1) + converted);
            }
        }
    }

    public static String formatChance(double chance) {
        if (chance == 0.0) return "§70%";
        if (chance >= 1.0)     return String.format("§a%.2f%%", chance);
        if (chance >= 0.01)    return String.format("§e%.4f%%", chance);
        if (chance >= 0.00001) return String.format("§6%.7f%%", chance);
        return String.format("§c%.9f%%", chance);
    }

    public static String formatFarmMulti(double multi) {
        return String.format("%.2f", multi) + "x";
    }

    public static String fmtCount(long value) {
        return NumberFormatter.format(new BigNumber((double) value));
    }
}
