package multigainer.multigainer.farming;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.ToolGUI;
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

import java.util.Arrays;
import java.util.List;

public class FarmingStorageGUI implements Listener {

    public static final String TITLE = "§fFarming Storage";

    // 36-slot layout (4 rows)
    // Row 0 (0-8):  border panes
    // Row 1 (9-17): [P] [S0] [S1] [S2] [S3] [S4] [S5] [S6] [P]
    // Row 2 (18-26): panes, AUTO-MERGE toggle at 22
    // Row 3 (27-35): BACK at 27, rest panes
    private static final int   TIER_COUNT  = FarmingManager.SEED_TIER_COUNT; // 7
    private static final int[] TIER_SLOTS  = { 10, 11, 12, 13, 14, 15, 16 };
    private static final int   SLOT_AUTO   = 22;
    private static final int   SLOT_BACK   = 31;

    private final Multigainer plugin;

    public FarmingStorageGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);
        ItemStack pane = makePane();
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        for (int tier = 0; tier < TIER_COUNT; tier++) {
            inv.setItem(TIER_SLOTS[tier], buildTierItem(profile, tier));
        }
        inv.setItem(SLOT_AUTO, buildAutoMergeButton(profile.isAutoMerge()));
        inv.setItem(SLOT_BACK, makeBack());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (slot == SLOT_BACK) {
            ToolGUI.open(player, profile, plugin);
            return;
        }

        if (slot == SLOT_AUTO) {
            boolean newState = !profile.isAutoMerge();
            profile.setAutoMerge(newState);
            event.getInventory().setItem(SLOT_AUTO, buildAutoMergeButton(newState));
            player.sendMessage("§7Auto-merge " + (newState ? "§anabled" : "§cdisabled") + "§7.");
            return;
        }

        for (int tier = 0; tier < TIER_COUNT; tier++) {
            if (slot == TIER_SLOTS[tier] && tier < TIER_COUNT - 1) {
                if (event.getClick() == ClickType.RIGHT) {
                    compressAll(profile, tier);
                } else {
                    compress64(profile, tier);
                }
                refreshInventory(profile, event.getInventory());
                return;
            }
        }
    }

    private void compress64(PlayerProfile profile, int tier) {
        long count = profile.getSeedStorage(tier);
        if (count < FarmingManager.COMPRESS_RATIO) return;
        profile.setSeedStorage(tier, count - FarmingManager.COMPRESS_RATIO);
        profile.setSeedStorage(tier + 1, profile.getSeedStorage(tier + 1) + 1);
    }

    private void compressAll(PlayerProfile profile, int tier) {
        long count     = profile.getSeedStorage(tier);
        long converted = count / FarmingManager.COMPRESS_RATIO;
        if (converted == 0) return;
        profile.setSeedStorage(tier, count % FarmingManager.COMPRESS_RATIO);
        profile.setSeedStorage(tier + 1, profile.getSeedStorage(tier + 1) + converted);
    }

    private void refreshInventory(PlayerProfile profile, Inventory inv) {
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            inv.setItem(TIER_SLOTS[tier], buildTierItem(profile, tier));
        }
    }

    private static ItemStack buildTierItem(PlayerProfile profile, int tier) {
        long count = profile.getSeedStorage(tier);
        ItemStack item = new ItemStack(FarmingManager.SEED_TIER_MATERIALS[tier]);
        ItemMeta meta  = item.getItemMeta();

        String tierColor = getTierColor(tier);
        meta.setDisplayName(tierColor + "§l" + FarmingManager.SEED_TIER_NAMES[tier]);

        String countStr = FarmingManager.fmtCount(count);
        if (tier < TIER_COUNT - 1) {
            long need = FarmingManager.COMPRESS_RATIO - (count % FarmingManager.COMPRESS_RATIO);
            meta.setLore(Arrays.asList(
                "§7Amount§8: §f" + countStr,
                "§7Until next§8: §e" + FarmingManager.fmtCount(need == FarmingManager.COMPRESS_RATIO ? 0 : need)
                    + " §8/ §e" + FarmingManager.COMPRESS_RATIO,
                "",
                "§7Left Click §8» §eCompress 64 → 1",
                "§7Right Click §8» §eCompress All"
            ));
        } else {
            meta.setLore(Arrays.asList(
                "§7Amount§8: §f" + countStr,
                "",
                "§7This is the highest tier."
            ));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildAutoMergeButton(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName((enabled ? "§a§l" : "§c§l") + "AUTO COMPRESSOR");
        meta.setLore(Arrays.asList(
            "§7Automatically compresses seeds",
            "§7into higher tiers on every harvest.",
            "",
            "§7Status§8: " + (enabled ? "§a✔ ON" : "§c✘ OFF"),
            "",
            "§eClick to toggle!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static String getTierColor(int tier) {
        return switch (tier) {
            case 0 -> "§e";
            case 1 -> "§f";
            case 2 -> "§6";
            case 3 -> "§7";
            case 4 -> "§c";
            case 5 -> "§b";
            case 6 -> "§5";
            default -> "§7";
        };
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName(" ");
        p.setItemMeta(m);
        return p;
    }

    private static ItemStack makeBack() {
        ItemStack p = new ItemStack(Material.ARROW);
        ItemMeta m  = p.getItemMeta();
        m.setDisplayName("§7« §fBack");
        m.setLore(List.of("§7Return to the Hoe Menu"));
        p.setItemMeta(m);
        return p;
    }
}
