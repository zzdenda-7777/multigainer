package multigainer.multigainer.artifacts;

import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.upgrades.UpgradeManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ArtifactManager {

    public static final String PDC_KEY = "multigainer_artifact";

    // All 70 artifacts ordered by type then tier (0 = weakest, 6 = strongest)
    private static final Map<String, ArtifactRecord> BY_ID = new LinkedHashMap<>();
    private static final List<ArtifactRecord> ALL = new ArrayList<>();

    public record ArtifactRecord(ArtifactType type, int tier, Material material, double multLog10) {
        public String id() { return type.name() + "_" + tier; }
    }

    static {
        // ── EXPONENT (§6) – planks ─────────────────────────────────────────────
        reg(ArtifactType.EXPONENT, 0, Material.BIRCH_PLANKS,          Math.log10(1.1));
        reg(ArtifactType.EXPONENT, 1, Material.OAK_PLANKS,            Math.log10(1.5));
        reg(ArtifactType.EXPONENT, 2, Material.JUNGLE_PLANKS,         Math.log10(2.0));
        reg(ArtifactType.EXPONENT, 3, Material.ACACIA_PLANKS,         Math.log10(3.5));
        reg(ArtifactType.EXPONENT, 4, Material.MANGROVE_PLANKS,       Math.log10(7.5));
        reg(ArtifactType.EXPONENT, 5, Material.SPRUCE_PLANKS,         Math.log10(25.0));
        reg(ArtifactType.EXPONENT, 6, Material.DARK_OAK_PLANKS,       2.0);             // ×100

        // ── GEM (§b) – stripped woods ──────────────────────────────────────────
        reg(ArtifactType.GEM, 0, Material.STRIPPED_BIRCH_WOOD,        Math.log10(2.0));
        reg(ArtifactType.GEM, 1, Material.STRIPPED_OAK_WOOD,          Math.log10(5.0));
        reg(ArtifactType.GEM, 2, Material.STRIPPED_JUNGLE_WOOD,       Math.log10(25.0));
        reg(ArtifactType.GEM, 3, Material.STRIPPED_ACACIA_WOOD,       Math.log10(150.0));
        reg(ArtifactType.GEM, 4, Material.STRIPPED_MANGROVE_WOOD,     Math.log10(2500.0));
        reg(ArtifactType.GEM, 5, Material.STRIPPED_SPRUCE_WOOD,       6.0);             // ×1M
        reg(ArtifactType.GEM, 6, Material.STRIPPED_DARK_OAK_WOOD,     Math.log10(5.0) + 12); // ×5T

        // ── FARM XP (§e) – sandstones ─────────────────────────────────────────
        reg(ArtifactType.FARM_XP, 0, Material.SANDSTONE,              Math.log10(1.5));
        reg(ArtifactType.FARM_XP, 1, Material.CHISELED_SANDSTONE,     Math.log10(4.0));
        reg(ArtifactType.FARM_XP, 2, Material.CUT_SANDSTONE,          1.0);             // ×10
        reg(ArtifactType.FARM_XP, 3, Material.SMOOTH_SANDSTONE,       Math.log10(50.0));
        reg(ArtifactType.FARM_XP, 4, Material.RED_SANDSTONE,          Math.log10(250.0));
        reg(ArtifactType.FARM_XP, 5, Material.CHISELED_RED_SANDSTONE, Math.log10(5000.0));
        reg(ArtifactType.FARM_XP, 6, Material.SMOOTH_RED_SANDSTONE,   Math.log10(250000.0));

        // ── MINE XP (§7) – stones ─────────────────────────────────────────────
        reg(ArtifactType.MINE_XP, 0, Material.STONE,                  Math.log10(1.5));
        reg(ArtifactType.MINE_XP, 1, Material.SMOOTH_STONE,           Math.log10(4.0));
        reg(ArtifactType.MINE_XP, 2, Material.STONE_BRICKS,           1.0);             // ×10
        reg(ArtifactType.MINE_XP, 3, Material.CHISELED_STONE_BRICKS,  Math.log10(50.0));
        reg(ArtifactType.MINE_XP, 4, Material.MOSSY_STONE_BRICKS,     Math.log10(250.0));
        reg(ArtifactType.MINE_XP, 5, Material.POLISHED_DEEPSLATE,     Math.log10(5000.0));
        reg(ArtifactType.MINE_XP, 6, Material.DEEPSLATE_BRICKS,       Math.log10(250000.0));

        // ── FARM MULTI (§6) – yellow blocks ──────────────────────────────────
        reg(ArtifactType.FARM_MULTI, 0, Material.YELLOW_CONCRETE_POWDER,    Math.log10(2.5));
        reg(ArtifactType.FARM_MULTI, 1, Material.YELLOW_WOOL,               Math.log10(50.0));
        reg(ArtifactType.FARM_MULTI, 2, Material.YELLOW_CONCRETE,           5.0);       // ×100K
        reg(ArtifactType.FARM_MULTI, 3, Material.YELLOW_TERRACOTTA,         12.0);      // ×1T
        reg(ArtifactType.FARM_MULTI, 4, Material.YELLOW_STAINED_GLASS,      21.0);      // ×1e21
        reg(ArtifactType.FARM_MULTI, 5, Material.END_STONE_BRICKS,          40.0);      // ×1e40
        reg(ArtifactType.FARM_MULTI, 6, Material.YELLOW_GLAZED_TERRACOTTA,  1000.0);    // ×1e1000

        // ── GRINDING POINTS (§c) – red blocks ────────────────────────────────
        reg(ArtifactType.GRINDING_POINTS, 0, Material.RED_CONCRETE_POWDER,  Math.log10(1.25));
        reg(ArtifactType.GRINDING_POINTS, 1, Material.RED_WOOL,             Math.log10(2.0));
        reg(ArtifactType.GRINDING_POINTS, 2, Material.RED_CONCRETE,         Math.log10(4.0));
        reg(ArtifactType.GRINDING_POINTS, 3, Material.RED_TERRACOTTA,       Math.log10(8.0));
        reg(ArtifactType.GRINDING_POINTS, 4, Material.RED_STAINED_GLASS,    Math.log10(20.0));
        reg(ArtifactType.GRINDING_POINTS, 5, Material.REDSTONE_BLOCK,       Math.log10(75.0));
        reg(ArtifactType.GRINDING_POINTS, 6, Material.RED_GLAZED_TERRACOTTA,Math.log10(500.0));

        // ── REBIRTH POINTS (§5) – purple blocks ──────────────────────────────
        reg(ArtifactType.REBIRTH_POINTS, 0, Material.PURPLE_CONCRETE_POWDER,  Math.log10(2.0));
        reg(ArtifactType.REBIRTH_POINTS, 1, Material.PURPLE_WOOL,             Math.log10(5.0));
        reg(ArtifactType.REBIRTH_POINTS, 2, Material.PURPLE_CONCRETE,         Math.log10(20.0));
        reg(ArtifactType.REBIRTH_POINTS, 3, Material.PURPLE_TERRACOTTA,       Math.log10(250.0));
        reg(ArtifactType.REBIRTH_POINTS, 4, Material.PURPLE_STAINED_GLASS,    Math.log10(50000.0));
        reg(ArtifactType.REBIRTH_POINTS, 5, Material.PURPUR_BLOCK,            9.0);     // ×1B
        reg(ArtifactType.REBIRTH_POINTS, 6, Material.PURPLE_GLAZED_TERRACOTTA,Math.log10(2.5) + 19.0); // ×2.5e19

        // ── TIER POINTS (§d) – bamboo / sponge ───────────────────────────────
        reg(ArtifactType.TIER_POINTS, 0, Material.STRIPPED_BAMBOO_BLOCK, Math.log10(1.1));
        reg(ArtifactType.TIER_POINTS, 1, Material.BAMBOO_PLANKS,         Math.log10(1.35));
        reg(ArtifactType.TIER_POINTS, 2, Material.BAMBOO_MOSAIC,         Math.log10(1.75));
        reg(ArtifactType.TIER_POINTS, 3, Material.SPONGE,               Math.log10(2.5));
        reg(ArtifactType.TIER_POINTS, 4, Material.HORN_CORAL_BLOCK,      Math.log10(4.0));
        reg(ArtifactType.TIER_POINTS, 5, Material.RAW_IRON_BLOCK,        Math.log10(8.0));
        reg(ArtifactType.TIER_POINTS, 6, Material.GOLD_BLOCK,            Math.log10(25.0));

        // ── SEED (§a) – lime blocks ───────────────────────────────────────────
        reg(ArtifactType.SEED, 0, Material.LIME_CONCRETE_POWDER,     Math.log10(1.25));
        reg(ArtifactType.SEED, 1, Material.LIME_WOOL,                Math.log10(1.75));
        reg(ArtifactType.SEED, 2, Material.LIME_CONCRETE,            Math.log10(2.5));
        reg(ArtifactType.SEED, 3, Material.LIME_TERRACOTTA,          Math.log10(4.0));
        reg(ArtifactType.SEED, 4, Material.LIME_STAINED_GLASS,       1.0);              // ×10
        reg(ArtifactType.SEED, 5, Material.SLIME_BLOCK,              Math.log10(20.0));
        reg(ArtifactType.SEED, 6, Material.LIME_GLAZED_TERRACOTTA,   Math.log10(50.0));

        // ── BLOCK STORAGE (§8) – basalt ───────────────────────────────────────
        reg(ArtifactType.BLOCK_STORAGE, 0, Material.POLISHED_BASALT,              Math.log10(1.15));
        reg(ArtifactType.BLOCK_STORAGE, 1, Material.BASALT,                       Math.log10(1.4));
        reg(ArtifactType.BLOCK_STORAGE, 2, Material.SMOOTH_BASALT,                Math.log10(2.0));
        reg(ArtifactType.BLOCK_STORAGE, 3, Material.BLACKSTONE,                   Math.log10(3.0));
        reg(ArtifactType.BLOCK_STORAGE, 4, Material.POLISHED_BLACKSTONE,          Math.log10(5.0));
        reg(ArtifactType.BLOCK_STORAGE, 5, Material.POLISHED_BLACKSTONE_BRICKS,   1.0);              // ×10
        reg(ArtifactType.BLOCK_STORAGE, 6, Material.CHISELED_POLISHED_BLACKSTONE, Math.log10(25.0));
    }

    private static void reg(ArtifactType type, int tier, Material mat, double multLog10) {
        ArtifactRecord r = new ArtifactRecord(type, tier, mat, multLog10);
        BY_ID.put(r.id(), r);
        ALL.add(r);
    }

    public static ArtifactRecord getById(String id) { return BY_ID.get(id); }
    public static List<ArtifactRecord> getAll() { return Collections.unmodifiableList(ALL); }

    // Combined BigNumber multiplier for a given type from all active slots
    public static BigNumber getMultiplier(PlayerProfile profile, ArtifactType type) {
        double log10 = 0;
        for (int i = 0; i < 3; i++) {
            String id = profile.getArtifactSlot(i);
            if (id == null || id.isEmpty()) continue;
            ArtifactRecord r = BY_ID.get(id);
            if (r != null && r.type() == type) log10 += r.multLog10();
        }
        return log10 == 0 ? new BigNumber(1.0) : UpgradeManager.fromLog10(log10);
    }

    // Double version for small multipliers (safe as long as result < 1e300)
    public static double getMultiplierDouble(PlayerProfile profile, ArtifactType type) {
        double log10 = 0;
        for (int i = 0; i < 3; i++) {
            String id = profile.getArtifactSlot(i);
            if (id == null || id.isEmpty()) continue;
            ArtifactRecord r = BY_ID.get(id);
            if (r != null && r.type() == type) log10 += r.multLog10();
        }
        return log10 == 0 ? 1.0 : Math.pow(10, log10);
    }

    // Build the artifact ItemStack with clean lore
    public static ItemStack buildItem(Plugin plugin, ArtifactRecord r) {
        return buildItem(plugin, r, null);
    }

    public static ItemStack buildItem(Plugin plugin, ArtifactRecord r, java.util.UUID uid) {
        ArtifactType type = r.type();
        BigNumber multBig = UpgradeManager.fromLog10(r.multLog10());
        String multStr = "§f×" + NumberFormatter.format(multBig, uid);

        ItemStack item = new ItemStack(r.material());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(type.color + "§l" + type.displayName.toUpperCase() + " ARTIFACT");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m──────────────────────");
            lore.add("  " + type.color + "§l" + multStr);
            lore.add("§8§m──────────────────────");
            lore.add("§7Multiplies your " + type.color + type.displayName);
            lore.add("§7by " + type.color + multStr + "§7 when placed");
            lore.add("§7in an §5/artifacts §7slot.");
            lore.add("§8§m──────────────────────");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING, r.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    // Identify artifact from item
    public static ArtifactRecord getFromItem(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING);
        return id != null ? BY_ID.get(id) : null;
    }

    public static boolean isArtifact(Plugin plugin, ItemStack item) {
        return getFromItem(plugin, item) != null;
    }
}
