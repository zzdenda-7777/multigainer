package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.farming.CropSelectionGUI;
import multigainer.multigainer.farming.EnchantToggleGUI;
import multigainer.multigainer.farming.FarmingManager;
import multigainer.multigainer.farming.FarmingStorageGUI;
import multigainer.multigainer.formatting.NumberFormatter;
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

    // 27-slot layout
    // Row 0: panes
    // Row 1: [P][STORAGE][P][P][HOE][P][P][CROP][P]
    // Row 2: [P][P][P][P][ENCHANT][P][P][P][P]
    private static final int SLOT_STORAGE = 10;
    private static final int SLOT_HOE     = 13;
    private static final int SLOT_CROP    = 16;
    private static final int SLOT_ENCHANT = 22;

    private final Multigainer plugin;

    public ToolGUI(Multigainer plugin) { this.plugin = plugin; }

    public void openHoeGUI(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        open(player, profile, plugin);
    }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, HOE_TITLE);
        ItemStack pane = makePane();
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // ── Slot 10: Farming Storage ──────────────────────────────
        BigNumber totalSeeds = new BigNumber(0);
        for (int t = 0; t < FarmingManager.SEED_TIER_COUNT; t++) {
            double raw = profile.getSeedStorage(t) * Math.pow(FarmingManager.COMPRESS_RATIO, t);
            totalSeeds = totalSeeds.add(new BigNumber(raw));
        }
        ItemStack storage = new ItemStack(Material.CHEST);
        ItemMeta sm = storage.getItemMeta();
        sm.setDisplayName("§b§lFARMING STORAGE");
        sm.setLore(Arrays.asList(
            "§7Stores all your seed currencies",
            "§7collected from farming.",
            "",
            "§7Total Seeds§8: §e" + NumberFormatter.format(totalSeeds),
            "",
            "§eClick to open farming storage!"
        ));
        storage.setItemMeta(sm);
        inv.setItem(SLOT_STORAGE, storage);

        // ── Slot 13: Hoe center display ───────────────────────────
        int hoeTier = profile.getHoeTier();
        ItemStack hoe = new ItemStack(FarmingManager.HOE_MATERIALS[hoeTier]);
        ItemMeta hm = hoe.getItemMeta();
        hm.setDisplayName(FarmingManager.HOE_TIER_COLORS[hoeTier] + "§lYOUR HOE");
        hm.setLore(buildHoeLore(profile));
        hoe.setItemMeta(hm);
        inv.setItem(SLOT_HOE, hoe);

        // ── Slot 16: Crop Selection ───────────────────────────────
        int chosen = profile.getChosenCrop();
        ItemStack cropBtn = new ItemStack(FarmingManager.CROP_DISPLAY_ITEMS[chosen]);
        ItemMeta cm = cropBtn.getItemMeta();
        cm.setDisplayName("§e§lCROP SELECTION");
        cm.setLore(Arrays.asList(
            "§7Current§8: " + FarmingManager.CROP_NAMES[chosen],
            "§7Seed Multi§8: §6×" + FarmingManager.fmtCount(FarmingManager.getSeedMultiplier(chosen)),
            "",
            "§eClick to choose a crop!"
        ));
        cropBtn.setItemMeta(cm);
        inv.setItem(SLOT_CROP, cropBtn);

        // ── Slot 22: Enchant Messages ─────────────────────────────
        ItemStack enchBtn = new ItemStack(Material.BOOK);
        ItemMeta em = enchBtn.getItemMeta();
        em.setDisplayName("§d§lENCHANT MESSAGES");
        em.setLore(Arrays.asList(
            "§7Toggle chat notifications",
            "§7for hoe enchant activations.",
            "",
            "§7💥 TNT: " + status(profile, 0),
            "§7💣 Nuke: " + status(profile, 1),
            "§7🌍 World Eater: " + status(profile, 2),
            "§7🌌 Universe Destroyer: " + status(profile, 3),
            "",
            "§eClick to manage!"
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
        if (slot < 0 || slot >= 27) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        switch (slot) {
            case SLOT_STORAGE -> FarmingStorageGUI.open(player, profile, plugin);
            case SLOT_CROP    -> CropSelectionGUI.open(player, profile, plugin);
            case SLOT_ENCHANT -> EnchantToggleGUI.open(player, profile, plugin);
        }
    }

    // Clean hoe lore with 2dp farm multi and formatted numbers
    public static List<String> buildHoeLore(PlayerProfile profile) {
        int    farmLevel = profile.getFarmingLevel();
        double farmMulti = profile.getFarmMulti();
        List<String> lore = new ArrayList<>();
        lore.add("§7Level: §e" + NumberFormatter.format(new BigNumber(farmLevel)));
        lore.add("§7Multi: §6" + FarmingManager.formatFarmMulti(farmMulti));
        lore.add("");
        lore.add("§c§lEnchants");
        lore.add("§7💥 TNT: §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(0, farmLevel)) + " §8(§c×10§8)");
        lore.add("§7💣 Nuke: §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(1, farmLevel)) + " §8(§c×250§8)");
        lore.add("§7🌍 World Eater:§f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(2, farmLevel)) + " §8(§c×7.5k§8)");
        lore.add("§7🌌 Universe Dest: §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(3, farmLevel)) + " §8(§c×100k§8)");
        lore.add("");
        lore.add("§eRight-click §7to open");
        return lore;
    }

    private static String status(PlayerProfile p, int i) {
        return p.isEnchantMessageEnabled(i) ? "§a✔" : "§c✘";
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName(" ");
        p.setItemMeta(m);
        return p;
    }
}
