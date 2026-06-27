package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.production.ProductionGUI;
import multigainer.multigainer.production.ProductionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PickaxeBlockStorageGUI implements Listener {

    public static final String TITLE = "§7Block Storage";

    private static final int SLOT_PRODUCTION = 4;
    private static final int SLOT_BACK       = 31;
    // Block item slots: 9 through 9+BLOCKS.length-1
    private static final int BLOCK_SLOT_START = 9;

    private final Multigainer plugin;

    public PickaxeBlockStorageGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        ItemStack pane = makePane(Material.GRAY_STAINED_GLASS_PANE);
        // Fill entire border (row 0 and row 3)
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 27; i < 36; i++) inv.setItem(i, pane);

        // Production shortcut at slot 4 (top row)
        inv.setItem(SLOT_PRODUCTION, makeProductionButton());

        // Place 17 block items in rows 1-2 (slots 9-25), slot 26 = glass
        for (int i = 0; i < PickaxeManager.BLOCKS.length; i++) {
            inv.setItem(BLOCK_SLOT_START + i, buildBlockItem(i, profile));
        }
        inv.setItem(26, pane);

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§7← Back");
        bm.setLore(List.of("§8Return to Pickaxe Menu"));
        back.setItemMeta(bm);
        inv.setItem(SLOT_BACK, back);

        player.openInventory(inv);
    }

    private static ItemStack buildBlockItem(int index, PlayerProfile profile) {
        Material mat  = PickaxeManager.BLOCKS[index];
        long count    = profile.getBlockStorage(index);
        String name   = PickaxeManager.BLOCK_NAMES[index];
        int minTier   = PickaxeManager.getMinTierForBlock(index);
        String tierColor = PickaxeManager.TIER_COLORS[minTier];
        String tierName  = PickaxeManager.TIER_NAMES[minTier];
        int xpPer     = ProductionManager.BLOCK_XP_VALUES[index];

        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§f§l" + name);
        m.setLore(Arrays.asList(
            "§7Stored: §f" + NumberFormatter.format(new BigNumber((double) count)),
            "§7XP per block: §a" + xpPer,
            "§7Tier: " + tierColor + tierName + " Pickaxe",
            " ",
            "§eLeft Click  §8→ §7Send 32 to Production",
            "§eRight Click §8→ §7Send All to Production"
        ));
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack makeProductionButton() {
        ItemStack item = new ItemStack(Material.PISTON);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName("§8⚙ §7Production");
            m.setLore(List.of(
                "§eLeft Click  §8→ §7Open Production GUI",
                "§eRight Click §8→ §7Send All Blocks to Production"
            ));
            item.setItemMeta(m);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (raw == SLOT_BACK) {
            PickaxeGUI.open(player, profile, plugin);
            return;
        }

        if (raw == SLOT_PRODUCTION) {
            if (event.getClick() == ClickType.RIGHT) {
                // Send ALL blocks from storage to production
                for (int i = 0; i < PickaxeManager.BLOCKS.length; i++) {
                    if (profile.getBlockStorage(i) > 0) {
                        ProductionManager.sendBlocksToProduction(player, profile, i, profile.getBlockStorage(i));
                    }
                }
                for (int i = 0; i < PickaxeManager.BLOCKS.length; i++) {
                    event.getInventory().setItem(BLOCK_SLOT_START + i, buildBlockItem(i, profile));
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> ProductionGUI.open(player, profile, plugin));
            }
            return;
        }

        // Block item click: left = send 32, right = send all
        int blockIndex = raw - BLOCK_SLOT_START;
        if (blockIndex >= 0 && blockIndex < PickaxeManager.BLOCKS.length) {
            long amount = event.getClick() == ClickType.RIGHT
                    ? profile.getBlockStorage(blockIndex)
                    : 32L;
            ProductionManager.sendBlocksToProduction(player, profile, blockIndex, amount);
            // Refresh that slot
            event.getInventory().setItem(raw, buildBlockItem(blockIndex, profile));
        }
    }

    private static ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);
        return pane;
    }

    private static String formatCount(long count) {
        if (count >= 1_000_000_000L) return String.format("%.2fB", count / 1_000_000_000.0);
        if (count >= 1_000_000L)     return String.format("%.2fM", count / 1_000_000.0);
        if (count >= 1_000L)         return String.format("%.2fK", count / 1_000.0);
        return NumberFormat.getNumberInstance(Locale.US).format(count);
    }
}
