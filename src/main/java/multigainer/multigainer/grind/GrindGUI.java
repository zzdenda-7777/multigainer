package multigainer.multigainer.grind;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GrindGUI implements Listener {

    private static final String TITLE = "§8✦ §aGrinding Points §8✦";

    // 54-slot layout matching UpgradeGUI aesthetic (black glass pane background)
    // Row 0 (0-8):   panes
    // Row 1 (9-17):  [p][U1][p][U2][p][U3][p][U4][p]
    // Row 2 (18-26): [p][U5][p][U6][p][U7][p][U8][p]
    // Row 3 (27-35): panes
    // Row 4 (36-44): [p][p][p][INFO][p][TOGGLE][p][p][p]
    // Row 5 (45-53): panes
    private static final int[] UPGRADE_SLOTS = { 10, 12, 14, 16, 19, 21, 23, 25 };
    private static final int SLOT_INFO   = 39;
    private static final int SLOT_TOGGLE = 41;

    private final Multigainer plugin;

    public GrindGUI(Multigainer plugin) { this.plugin = plugin; }

    public void open(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) { player.sendMessage("§cProfile loading, try again!"); return; }

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        // Fill with black panes like UpgradeGUI
        ItemStack pane = makePane();
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        inv.setItem(UPGRADE_SLOTS[0], buildChanceItem(profile));
        inv.setItem(UPGRADE_SLOTS[1], buildExponentItem(profile));
        inv.setItem(UPGRADE_SLOTS[2], buildFarmMultiItem(profile));
        inv.setItem(UPGRADE_SLOTS[3], buildGemMultiItem(profile));
        inv.setItem(UPGRADE_SLOTS[4], buildFarmXpItem(profile));
        inv.setItem(UPGRADE_SLOTS[5], buildMineXpItem(profile));
        inv.setItem(UPGRADE_SLOTS[6], buildSeedMultiItem(profile));
        inv.setItem(UPGRADE_SLOTS[7], buildGPMultiItem(profile));
        inv.setItem(SLOT_INFO,   buildInfoItem(profile));
        inv.setItem(SLOT_TOGGLE, buildToggleItem(profile));

        player.openInventory(inv);
    }

    // ── Upgrade item builders (brief lore matching UpgradeGUI style) ──────────

    private ItemStack buildChanceItem(PlayerProfile profile) {
        int lvl = profile.getGrindChanceLevel();
        int next = lvl + 1;
        double cost = GrindManager.getChanceCost(next);
        return buildUpgradeItem(
            Material.PAPER,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Chance Reduction",
            lvl,
            String.format("Farm %.2f%%  Mine %.2f%%",
                GrindManager.getFarmingChancePct(lvl), GrindManager.getMiningChancePct(lvl)),
            String.format("Farm %.2f%%  Mine %.2f%%",
                GrindManager.getFarmingChancePct(next), GrindManager.getMiningChancePct(next)),
            cost
        );
    }

    private ItemStack buildExponentItem(PlayerProfile profile) {
        int lvl = profile.getGrindExponentLevel();
        int next = lvl + 1;
        double cost = GrindManager.getExponentCost(next);
        return buildUpgradeItem(
            Material.NETHER_STAR,
            ChatColor.GOLD + "" + ChatColor.BOLD + "Money Exponent",
            lvl,
            String.format("Exponent: %.2f", GrindManager.getMoneyExponent(lvl)),
            String.format("Exponent: %.2f (+0.01)", GrindManager.getMoneyExponent(next)),
            cost
        );
    }

    private ItemStack buildFarmMultiItem(PlayerProfile profile) {
        int lvl = profile.getGrindFarmMultiLevel();
        int next = lvl + 1;
        double cost = GrindManager.getFarmMultiCost(next);
        return buildUpgradeItem(
            Material.HAY_BLOCK,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Farm Multi",
            lvl,
            String.format("Multi: %.4fx", GrindManager.getFarmMulti(lvl)),
            String.format("%.4fx (×1.15)", GrindManager.getFarmMulti(next)),
            cost
        );
    }

    private ItemStack buildGemMultiItem(PlayerProfile profile) {
        int lvl = profile.getGrindGemMultiLevel();
        int next = lvl + 1;
        double cost = GrindManager.getGemMultiCost(next);
        return buildUpgradeItem(
            Material.DIAMOND,
            ChatColor.AQUA + "" + ChatColor.BOLD + "Gem Multi",
            lvl,
            String.format("Multi: %.4fx", GrindManager.getGemMulti(lvl)),
            String.format("%.4fx (×1.05)", GrindManager.getGemMulti(next)),
            cost
        );
    }

    private ItemStack buildFarmXpItem(PlayerProfile profile) {
        int lvl = profile.getGrindFarmXpLevel();
        int next = lvl + 1;
        double cost = GrindManager.getFarmXpCost(next);
        return buildUpgradeItem(
            Material.GOLDEN_CARROT,
            ChatColor.YELLOW + "" + ChatColor.BOLD + "Farm XP Boost",
            lvl,
            String.format("XP Multi: %.4fx", GrindManager.getFarmXpMulti(lvl)),
            String.format("%.4fx (×1.075)", GrindManager.getFarmXpMulti(next)),
            cost
        );
    }

    private ItemStack buildMineXpItem(PlayerProfile profile) {
        int lvl = profile.getGrindMineXpLevel();
        int next = lvl + 1;
        double cost = GrindManager.getMineXpCost(next);
        return buildUpgradeItem(
            Material.IRON_ORE,
            ChatColor.GRAY + "" + ChatColor.BOLD + "Mine XP Boost",
            lvl,
            String.format("XP Multi: %.4fx", GrindManager.getMineXpMulti(lvl)),
            String.format("%.4fx (×1.04)", GrindManager.getMineXpMulti(next)),
            cost
        );
    }

    private ItemStack buildSeedMultiItem(PlayerProfile profile) {
        int lvl = profile.getGrindSeedMultiLevel();
        int next = lvl + 1;
        double cost = GrindManager.getSeedMultiCost(next);
        return buildUpgradeItem(
            Material.WHEAT_SEEDS,
            ChatColor.GREEN + "" + ChatColor.BOLD + "Seed Multi",
            lvl,
            String.format("Multi: %.4fx", GrindManager.getSeedMulti(lvl)),
            String.format("%.4fx (×1.03)", GrindManager.getSeedMulti(next)),
            cost
        );
    }

    private ItemStack buildGPMultiItem(PlayerProfile profile) {
        int lvl = profile.getGrindGPMultiLevel();
        int next = lvl + 1;
        double cost = GrindManager.getGPMultiCost(next);
        return buildUpgradeItem(
            Material.QUARTZ,
            ChatColor.WHITE + "" + ChatColor.BOLD + "GP Multi",
            lvl,
            String.format("Multi: %.4fx", GrindManager.getGPMulti(lvl)),
            String.format("%.4fx (×1.25)", GrindManager.getGPMulti(next)),
            cost
        );
    }

    // Shared builder matching UpgradeGUI lore layout
    private static ItemStack buildUpgradeItem(Material mat, String displayName,
                                              int lvl, String currentStr, String nextStr, double cost) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + lvl);
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + currentStr);
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Next Level: " + ChatColor.AQUA + nextStr);
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + NumberFormatter.format(new BigNumber(cost)) + " GP");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to upgrade!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildInfoItem(PlayerProfile profile) {
        double gp = profile.getGrindingPoints();
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Grinding Points");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + NumberFormatter.format(new BigNumber(gp)));
        lore.add(ChatColor.GRAY + "Farm chance: " + ChatColor.WHITE + String.format("%.2f%%", GrindManager.getFarmingChancePct(profile.getGrindChanceLevel())));
        lore.add(ChatColor.GRAY + "Mine chance: " + ChatColor.WHITE + String.format("%.2f%%", GrindManager.getMiningChancePct(profile.getGrindChanceLevel())));
        lore.add(" ");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildToggleItem(PlayerProfile profile) {
        boolean enabled = profile.isGrindMessagesEnabled();
        ItemStack item = new ItemStack(enabled ? Material.GREEN_DYE : Material.RED_DYE);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "GP Messages");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Status: " + (enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to toggle!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Click handler ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        int slot = event.getRawSlot();

        for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
            if (slot == UPGRADE_SLOTS[i]) { handleUpgrade(player, profile, i); return; }
        }
        if (slot == SLOT_TOGGLE) handleToggle(player, profile);
    }

    private void handleUpgrade(Player player, PlayerProfile profile, int upgradeIndex) {
        int    nextLevel = getLevel(profile, upgradeIndex) + 1;
        double cost      = getCost(upgradeIndex, nextLevel);

        if (profile.getGrindingPoints() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough GP! Need "
                    + ChatColor.AQUA + NumberFormatter.format(new BigNumber(cost)) + " GP§c.");
            return;
        }

        profile.setGrindingPoints(profile.getGrindingPoints() - cost);
        setLevel(profile, upgradeIndex, nextLevel);

        player.sendMessage(ChatColor.GREEN + getUpgradeName(upgradeIndex)
                + " upgraded to level " + nextLevel + "!");
        open(player);
    }

    private void handleToggle(Player player, PlayerProfile profile) {
        boolean newState = !profile.isGrindMessagesEnabled();
        profile.setGrindMessagesEnabled(newState);
        player.sendMessage(ChatColor.GRAY + "GP messages " + (newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GRAY + ".");
        open(player);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getLevel(PlayerProfile p, int i) {
        return switch (i) {
            case 0 -> p.getGrindChanceLevel();
            case 1 -> p.getGrindExponentLevel();
            case 2 -> p.getGrindFarmMultiLevel();
            case 3 -> p.getGrindGemMultiLevel();
            case 4 -> p.getGrindFarmXpLevel();
            case 5 -> p.getGrindMineXpLevel();
            case 6 -> p.getGrindSeedMultiLevel();
            case 7 -> p.getGrindGPMultiLevel();
            default -> 0;
        };
    }

    private void setLevel(PlayerProfile p, int i, int lvl) {
        switch (i) {
            case 0 -> p.setGrindChanceLevel(lvl);
            case 1 -> p.setGrindExponentLevel(lvl);
            case 2 -> p.setGrindFarmMultiLevel(lvl);
            case 3 -> p.setGrindGemMultiLevel(lvl);
            case 4 -> p.setGrindFarmXpLevel(lvl);
            case 5 -> p.setGrindMineXpLevel(lvl);
            case 6 -> p.setGrindSeedMultiLevel(lvl);
            case 7 -> p.setGrindGPMultiLevel(lvl);
        }
    }

    private double getCost(int i, int nextLevel) {
        return switch (i) {
            case 0 -> GrindManager.getChanceCost(nextLevel);
            case 1 -> GrindManager.getExponentCost(nextLevel);
            case 2 -> GrindManager.getFarmMultiCost(nextLevel);
            case 3 -> GrindManager.getGemMultiCost(nextLevel);
            case 4 -> GrindManager.getFarmXpCost(nextLevel);
            case 5 -> GrindManager.getMineXpCost(nextLevel);
            case 6 -> GrindManager.getSeedMultiCost(nextLevel);
            case 7 -> GrindManager.getGPMultiCost(nextLevel);
            default -> Double.MAX_VALUE;
        };
    }

    private String getUpgradeName(int i) {
        return switch (i) {
            case 0 -> "Chance Reduction";
            case 1 -> "Money Exponent";
            case 2 -> "Farm Multi";
            case 3 -> "Gem Multi";
            case 4 -> "Farm XP";
            case 5 -> "Mine XP";
            case 6 -> "Seed Multi";
            case 7 -> "GP Multi";
            default -> "Upgrade";
        };
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m  = p.getItemMeta();
        if (m != null) { m.setDisplayName(" "); p.setItemMeta(m); }
        return p;
    }
}
