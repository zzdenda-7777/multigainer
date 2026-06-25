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

    public static final String HOE_TITLE = "§8🌾 §fHoe Menu";

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
        sm.setDisplayName("§b§lFarming Storage");
        sm.setLore(Arrays.asList(
            "§8———————————————",
            "§7Stores all your seed currencies",
            "§7collected from farming.",
            "§8———————————————",
            "§7Total Seeds§8: §e" + NumberFormatter.format(totalSeeds),
            "§8———————————————",
            "§eClick to open!"
        ));
        storage.setItemMeta(sm);
        inv.setItem(SLOT_STORAGE, storage);

        // ── Slot 13: Hoe center display ───────────────────────────
        int hoeTier = profile.getHoeTier();
        ItemStack hoe = new ItemStack(FarmingManager.HOE_MATERIALS[hoeTier]);
        ItemMeta hm = hoe.getItemMeta();
        hm.setDisplayName(FarmingManager.HOE_TIER_COLORS[hoeTier] + "§l✦ Multigainer Hoe ✦");
        hm.setLore(buildHoeLore(profile));
        hoe.setItemMeta(hm);
        inv.setItem(SLOT_HOE, hoe);

        // ── Slot 16: Crop Selection ───────────────────────────────
        int chosen = profile.getChosenCrop();
        ItemStack cropBtn = new ItemStack(FarmingManager.CROP_DISPLAY_ITEMS[chosen]);
        ItemMeta cm = cropBtn.getItemMeta();
        cm.setDisplayName("§e§lCrop Selection");
        cm.setLore(Arrays.asList(
            "§8———————————————",
            "§7Current§8:    " + FarmingManager.CROP_NAMES[chosen],
            "§7Seed Multi§8: §6" + FarmingManager.fmtCount(FarmingManager.getSeedMultiplier(chosen)) + "x",
            "§8———————————————",
            "§eClick to choose a crop!"
        ));
        cropBtn.setItemMeta(cm);
        inv.setItem(SLOT_CROP, cropBtn);

        // ── Slot 22: Enchant Messages ─────────────────────────────
        ItemStack enchBtn = new ItemStack(Material.BOOK);
        ItemMeta em = enchBtn.getItemMeta();
        em.setDisplayName("§d§lEnchant Messages");
        em.setLore(Arrays.asList(
            "§8———————————————",
            "§7Toggle chat notifications",
            "§7for hoe enchant activations.",
            "§8———————————————",
            "§7💥 TNT §8» " + status(profile, 0),
            "§7💣 Nuke §8» " + status(profile, 1),
            "§7🌍 World Eater §8» " + status(profile, 2),
            "§7🌌 Univ. Destroyer §8» " + status(profile, 3),
            "§8———————————————",
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
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§7Level §8│ §e" + NumberFormatter.format(new BigNumber(farmLevel)));
        lore.add("§7Multi §8│ §6" + FarmingManager.formatFarmMulti(farmMulti));
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§c§l⚡ Enchants");
        lore.add("§8  §7💥 TNT         §8│ §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(0, farmLevel)) + " §8│ §c+10x");
        lore.add("§8  §7💣 Nuke        §8│ §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(1, farmLevel)) + " §8│ §c+250x");
        lore.add("§8  §7🌍 World Eater §8│ §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(2, farmLevel)) + " §8│ §c+7.5k");
        lore.add("§8  §7🌌 Univ. Dest  §8│ §f" + FarmingManager.formatChance(FarmingManager.getEnchantChance(3, farmLevel)) + " §8│ §c+100k");
        lore.add("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        lore.add("§8» §eRight-click §7to open");
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
