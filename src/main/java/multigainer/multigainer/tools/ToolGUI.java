package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.farming.CropSelectionGUI;
import multigainer.multigainer.farming.EnchantToggleGUI;
import multigainer.multigainer.farming.FarmingManager;
import multigainer.multigainer.farming.FarmingStorageGUI;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.grind.GrindGUI;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ToolGUI implements Listener {

    public static final String HOE_TITLE = "§fHoe Menu";

    private static final int SLOT_STORAGE = 11;
    private static final int SLOT_HOE     = 13;
    private static final int SLOT_CROP    = 15;
    private static final int SLOT_GRIND   = 21;
    private static final int SLOT_ENCHANT = 23;

    private final Multigainer plugin;

    public ToolGUI(Multigainer plugin) { this.plugin = plugin; }

    public void openHoeGUI(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        open(player, profile, plugin);
    }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, HOE_TITLE);
        ItemStack pane = makePane();
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        // ── Slot 11: Farming Storage ──────────────────────────────
        BigNumber totalSeeds = new BigNumber(0);
        for (int t = 0; t < FarmingManager.SEED_TIER_COUNT; t++) {
            double raw = profile.getSeedStorage(t) * Math.pow(FarmingManager.COMPRESS_RATIO, t);
            totalSeeds = totalSeeds.add(new BigNumber(raw));
        }
        ItemStack storage = new ItemStack(Material.CHEST);
        ItemMeta sm = storage.getItemMeta();
        sm.setDisplayName("§b§lFARMING STORAGE");
        sm.setLore(Arrays.asList(
            "§8§m──────────────────────",
            "§7Stores all your seed currencies",
            "§7collected from farming.",
            "§8 ",
            "§7Total Seeds§8: §e" + NumberFormatter.format(totalSeeds),
            "§8§m──────────────────────",
            "§e▶ §7Click to open storage"
        ));
        storage.setItemMeta(sm);
        inv.setItem(SLOT_STORAGE, storage);

        // ── Slot 13: Hoe center display (detailed GUI lore) ───────
        int hoeTier = profile.getHoeTier();
        ItemStack hoe = new ItemStack(FarmingManager.HOE_MATERIALS[hoeTier]);
        ItemMeta hm = hoe.getItemMeta();
        hm.setDisplayName(FarmingManager.HOE_TIER_COLORS[hoeTier] + "§l"
                + FarmingManager.HOE_TIER_NAMES[hoeTier].toUpperCase() + " HOE");
        hm.setLore(buildGuiHoeLore(profile));
        hoe.setItemMeta(hm);
        inv.setItem(SLOT_HOE, hoe);

        // ── Slot 15: Crop Selection ───────────────────────────────
        int chosen = profile.getChosenCrop();
        ItemStack cropBtn = new ItemStack(FarmingManager.CROP_DISPLAY_ITEMS[chosen]);
        ItemMeta cm = cropBtn.getItemMeta();
        cm.setDisplayName("§e§lCROP SELECTION");
        cm.setLore(Arrays.asList(
            "§8§m──────────────────────",
            "§7Current§8: " + FarmingManager.CROP_NAMES[chosen],
            "§7Seed Multi§8: §6×" + FarmingManager.fmtCount(FarmingManager.getSeedMultiplier(chosen)),
            "§8§m──────────────────────",
            "§e▶ §7Click to choose a crop!"
        ));
        cropBtn.setItemMeta(cm);
        inv.setItem(SLOT_CROP, cropBtn);

        // ── Slot 21: Grind ─────────────────────────────────────────
        ItemStack grindBtn = new ItemStack(Material.QUARTZ);
        ItemMeta grm = grindBtn.getItemMeta();
        grm.setDisplayName("§a§lGRINDING POINTS");
        grm.setLore(Arrays.asList(
            "§8§m──────────────────────",
            "§7Spend your Grinding Points",
            "§7on permanent upgrades.",
            "§8 ",
            "§7Balance§8: §e" + NumberFormatter.format(new BigNumber(profile.getGrindingPoints())) + " GP",
            "§8§m──────────────────────",
            "§e▶ §7Click to open!"
        ));
        grindBtn.setItemMeta(grm);
        inv.setItem(SLOT_GRIND, grindBtn);

        // ── Slot 23: Enchant Messages ─────────────────────────────
        ItemStack enchBtn = new ItemStack(Material.BOOK);
        ItemMeta em = enchBtn.getItemMeta();
        em.setDisplayName("§d§lENCHANT MESSAGES");
        em.setLore(Arrays.asList(
            "§8§m──────────────────────",
            "§7Toggle chat notifications",
            "§7for hoe enchant activations.",
            "§8 ",
            "§7💥 TNT§8: " + status(profile, 0),
            "§7💣 Nuke§8: " + status(profile, 1),
            "§7🌍 World Eater§8: " + status(profile, 2),
            "§7🌌 Universe Destroyer§8: " + status(profile, 3),
            "§8§m──────────────────────",
            "§e▶ §7Click to manage!"
        ));
        enchBtn.setItemMeta(em);
        inv.setItem(SLOT_ENCHANT, enchBtn);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(HOE_TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        switch (slot) {
            case SLOT_STORAGE -> FarmingStorageGUI.open(player, profile, plugin);
            case SLOT_CROP    -> CropSelectionGUI.open(player, profile, plugin);
            case SLOT_GRIND   -> plugin.getGrindGUI().open(player);
            case SLOT_ENCHANT -> EnchantToggleGUI.open(player, profile, plugin);
        }
    }

    // ── Lore for inventory hover (compact) ────────────────────────────────────
    public static List<String> buildInventoryHoeLore(PlayerProfile profile) {
        int    farmLevel = profile.getFarmingLevel();
        double farmMulti = profile.getFarmMulti();
        List<String> lore = new ArrayList<>();
        lore.add("§8§m──────────────────────");
        lore.add("§7Farm Level§8: §e" + NumberFormatter.format(new BigNumber(farmLevel)));
        lore.add("§7Farm Multi§8: §6" + NumberFormatter.format(new BigNumber(farmMulti)) + "x");
        lore.add("§8§m──────────────────────");
        lore.add("§c✦ §lEnchant Chances");
        lore.add("§7💥 TNT§8:              §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(0, farmLevel)) + " §8(§cx10§8)");
        lore.add("§7💣 Nuke§8:             §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(1, farmLevel)) + " §8(§cx250§8)");
        lore.add("§7🌍 World Eater§8:      §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(2, farmLevel)) + " §8(§cx7.5k§8)");
        lore.add("§7🌌 Universe Dest§8:    §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(3, farmLevel)) + " §8(§cx100k§8)");
        lore.add("§8§m──────────────────────");
        lore.add("§e▶ §7Right-click to open menu");
        return lore;
    }

    // ── Lore for GUI slot 13 (detailed) ──────────────────────────────────────
    public static List<String> buildGuiHoeLore(PlayerProfile profile) {
        int    farmLevel = profile.getFarmingLevel();
        double farmMulti = profile.getFarmMulti();
        List<String> lore = new ArrayList<>();
        lore.add("§8§m══════════════════════");
        lore.add("§e⭐ §f§lFarming Tool Information");
        lore.add("§8§m══════════════════════");
        lore.add("§7Farm Level§8:  §a§l" + NumberFormatter.format(new BigNumber(farmLevel)));
        lore.add("§7Farm Multi§8:  §6§l" + NumberFormatter.format(new BigNumber(farmMulti)) + "x");
        lore.add("§8 ");
        lore.add("§c§l✦ ENCHANT DETAILS");
        lore.add("§8§m──────────────────────");
        addEnchantDetail(lore, "💥 TNT",              FarmingManager.getEnchantChance(0, farmLevel), "×10",     farmLevel);
        addEnchantDetail(lore, "💣 Nuke",             FarmingManager.getEnchantChance(1, farmLevel), "×250",    farmLevel);
        addEnchantDetail(lore, "🌍 World Eater",      FarmingManager.getEnchantChance(2, farmLevel), "×7,500",  farmLevel);
        addEnchantDetail(lore, "🌌 Universe Destroyer", FarmingManager.getEnchantChance(3, farmLevel), "×100,000", farmLevel);
        lore.add("§8§m══════════════════════");
        return lore;
    }

    private static void addEnchantDetail(List<String> lore, String name, double chance, String reward, int farmLevel) {
        lore.add("§7" + name);
        lore.add("  §8└ §7Chance§8: §f" + FarmingManager.formatChance(chance));
        lore.add("  §8└ §7Reward§8: §c" + reward + " §7seeds");
    }

    // Keep backward-compat alias (used nowhere else now, but safe to keep)
    public static List<String> buildHoeLore(PlayerProfile profile) {
        return buildInventoryHoeLore(profile);
    }

    private static String status(PlayerProfile p, int i) {
        return p.isEnchantMessageEnabled(i) ? "§a✔ ON" : "§c✘ OFF";
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName(" ");
        p.setItemMeta(m);
        return p;
    }
}
