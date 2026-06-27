package multigainer.multigainer.perks;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.PickaxeGUI;
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

public class PerkGUI implements Listener {

    // ── GUI titles ─────────────────────────────────────────────────────────────
    public static final String TITLE_NAV      = "§5✦ §dPerks Menu §5✦";
    public static final String TITLE_STATUS   = "§5✦ §dPerk Status §5✦";
    public static final String TITLE_UPGRADES = "§5✦ §dUpgrade Chances §5✦";
    public static final String TITLE_MESSAGES = "§5✦ §dPerk Messages §5✦";

    // ── Nav GUI slots (27) ─────────────────────────────────────────────────────
    private static final int NAV_SLOT_STATUS   = 10;
    private static final int NAV_SLOT_UPGRADES = 12;
    private static final int NAV_SLOT_INFO     = 14;
    private static final int NAV_SLOT_MESSAGES = 16;
    private static final int NAV_SLOT_BACK     = 22;

    // ── Status GUI slots (36) ──────────────────────────────────────────────────
    private static final int[] STATUS_SLOTS = {11, 13, 15, 21, 23};
    private static final int   STATUS_BACK  = 31;

    // ── Upgrade GUI slots (54) ─────────────────────────────────────────────────
    private static final int[] UPGRADE_SLOTS = {10, 12, 14, 16, 31};
    private static final int   UPGRADE_GP_INFO = 40;
    private static final int   UPGRADE_BACK    = 49;

    // ── Messages GUI slots (27) ────────────────────────────────────────────────
    private static final int[] MSG_SLOTS = {10, 12, 14, 21, 23};
    private static final int   MSG_BACK  = 26;

    private static final Material[] PERK_MATERIALS = {
        Material.EMERALD, Material.GOLD_INGOT, Material.AMETHYST_SHARD,
        Material.DIAMOND, Material.NETHER_STAR
    };

    private final Multigainer plugin;

    public PerkGUI(Multigainer plugin) { this.plugin = plugin; }

    // ── Open: Nav ──────────────────────────────────────────────────────────────
    public static void openNav(Player player, PlayerProfile profile, Multigainer plugin) {
        java.util.UUID uid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_NAV);
        ItemStack pane = makePane();
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        BigNumber totalMulti = PerkManager.getTotalPerkMultiplierBig(profile.getPerkCounts());

        inv.setItem(NAV_SLOT_STATUS, buildNavItem(
            Material.NETHER_STAR, "§d§lPerk Progress",
            List.of("§8═══════════════════════",
                "§7View all 5 perks and their",
                "§7current multiplier values.",
                "§8 ",
                "§7Total Perk Multi: §f" + NumberFormatter.format(totalMulti, uid) + "x",
                "§8═══════════════════════",
                "§e▶ Click to view!")));

        inv.setItem(NAV_SLOT_UPGRADES, buildNavItem(
            Material.DIAMOND, "§a§lUpgrade Chances",
            List.of("§8═══════════════════════",
                "§7Spend Grinding Points to",
                "§7improve your perk drop chances.",
                "§8 ",
                "§7Each upgrade: §c-2% §7compound",
                "§8═══════════════════════",
                "§e▶ Click to upgrade!")));

        inv.setItem(NAV_SLOT_INFO, buildNavItem(
            Material.BOOK, "§b§lHow Perks Work",
            List.of("§8═══════════════════════",
                "§6§lPERK SYSTEM INFO",
                "§8═══════════════════════",
                "§7✦ §fPerks §7boost your §6money income",
                "§7  from all sources.",
                "§8 ",
                "§7✦ §fEach perk you find gives a",
                "§7  §fmultiply §7on top of existing ones",
                "§7  §8(all perks multiply each other§8).",
                "§8 ",
                "§7✦ §fThe more perks you have, the",
                "§7  §fexponentially §7stronger the boost.",
                "§8 ",
                "§7✦ §fPerks are obtained §aonly §7from",
                "§7  §fmining §7(Tier 5+ required).",
                "§8═══════════════════════")));

        inv.setItem(NAV_SLOT_MESSAGES, buildNavItem(
            Material.PAPER, "§b§lMessage Settings",
            List.of("§8═══════════════════════",
                "§7Toggle chat notifications",
                "§7for each perk individually.",
                "§8═══════════════════════",
                "§e▶ Click to manage!")));

        inv.setItem(NAV_SLOT_BACK, makeBack(""));
        player.openInventory(inv);
    }

    // ── Open: Status ───────────────────────────────────────────────────────────
    public static void openStatus(Player player, PlayerProfile profile, Multigainer plugin) {
        java.util.UUID uid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_STATUS);
        ItemStack pane = makePane();
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        for (int i = 0; i < PerkManager.PERK_COUNT; i++) {
            inv.setItem(STATUS_SLOTS[i], buildStatusItem(i, profile, uid));
        }
        inv.setItem(STATUS_BACK, makeBack(TITLE_NAV));
        player.openInventory(inv);
    }

    // ── Open: Upgrades ─────────────────────────────────────────────────────────
    public static void openUpgrades(Player player, PlayerProfile profile, Multigainer plugin) {
        java.util.UUID uid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_UPGRADES);
        ItemStack pane = makePane();
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        for (int i = 0; i < PerkManager.PERK_COUNT; i++) {
            inv.setItem(UPGRADE_SLOTS[i], buildUpgradeItem(i, profile, uid));
        }

        // GP balance info
        ItemStack gpItem = new ItemStack(Material.LIME_DYE);
        ItemMeta gm = gpItem.getItemMeta();
        gm.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Grinding Points");
        List<String> gl = new ArrayList<>();
        gl.add(" ");
        gl.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + NumberFormatter.format(new BigNumber(profile.getGrindingPoints()), uid) + " GP");
        gl.add(" ");
        gm.setLore(gl);
        gpItem.setItemMeta(gm);
        inv.setItem(UPGRADE_GP_INFO, gpItem);

        inv.setItem(UPGRADE_BACK, makeBack(TITLE_NAV));
        player.openInventory(inv);
    }

    // ── Open: Messages ─────────────────────────────────────────────────────────
    public static void openMessages(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MESSAGES);
        ItemStack pane = makePane();
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        for (int i = 0; i < PerkManager.PERK_COUNT; i++) {
            inv.setItem(MSG_SLOTS[i], buildMessageItem(i, profile));
        }
        inv.setItem(MSG_BACK, makeBack(TITLE_NAV));
        player.openInventory(inv);
    }

    // ── Item builders ──────────────────────────────────────────────────────────

    private static ItemStack buildStatusItem(int idx, PlayerProfile profile) {
        return buildStatusItem(idx, profile, null);
    }

    private static ItemStack buildStatusItem(int idx, PlayerProfile profile, java.util.UUID uid) {
        int count = profile.getPerkCount(idx);
        double denom = PerkManager.getPerkChanceDenominator(idx, profile.getPerkChanceLevel(idx));
        BigNumber multi = PerkManager.getPerkMultiplierBig(idx, count);

        ItemStack item = new ItemStack(PERK_MATERIALS[idx]);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PerkManager.PERK_COLORS[idx] + "§l" + PerkManager.PERK_NAMES[idx]);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Times Found: " + ChatColor.WHITE + NumberFormatter.format(new BigNumber(count), uid));
        lore.add(ChatColor.GRAY + "Your Multi:  " + ChatColor.WHITE + NumberFormatter.format(multi, uid) + "x");
        lore.add(ChatColor.GRAY + "Base Rate:   " + ChatColor.AQUA + "x" + PerkManager.PERK_BASE_MULTIPLIERS[idx] + " §7per find");
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Drop Chance: " + ChatColor.YELLOW + "1/" + String.format("%.2f", denom));
        lore.add(ChatColor.GRAY + "Chance Upg Level: " + ChatColor.WHITE + profile.getPerkChanceLevel(idx));
        lore.add(" ");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildUpgradeItem(int idx, PlayerProfile profile) {
        return buildUpgradeItem(idx, profile, null);
    }

    private static ItemStack buildUpgradeItem(int idx, PlayerProfile profile, java.util.UUID uid) {
        int lvl  = profile.getPerkChanceLevel(idx);
        int next = lvl + 1;
        double currentDenom = PerkManager.getPerkChanceDenominator(idx, lvl);
        double nextDenom    = PerkManager.getPerkChanceDenominator(idx, next);
        double cost         = PerkManager.getPerkUpgradeCost(next);

        ItemStack item = new ItemStack(PERK_MATERIALS[idx]);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(PerkManager.PERK_COLORS[idx] + "§l" + PerkManager.PERK_NAMES[idx] + " §7- Chance Upgrade");

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + lvl);
        lore.add(ChatColor.GRAY + "Current Chance: " + ChatColor.WHITE + "1/" + String.format("%.2f", currentDenom));
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Next Level: " + ChatColor.AQUA + "1/" + String.format("%.2f", nextDenom) + " §8(-2%)");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + NumberFormatter.format(new BigNumber(cost), uid) + " GP");
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to upgrade!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildMessageItem(int idx, PlayerProfile profile) {
        boolean enabled = profile.isPerkMessageEnabled(idx);
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? "§a§l" : "§7§l") + PerkManager.PERK_NAMES[idx] + " Messages");

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Perk " + (idx + 1) + " §8│ §7Base: " + PerkManager.PERK_COLORS[idx] + "x" + PerkManager.PERK_BASE_MULTIPLIERS[idx]);
        lore.add(ChatColor.GRAY + "Drop Chance: " + ChatColor.YELLOW + "1/" + String.format("%.2f", PerkManager.getPerkChanceDenominator(idx, profile.getPerkChanceLevel(idx))));
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Chat Message: " + (enabled ? ChatColor.GREEN + "✔ Enabled" : ChatColor.RED + "✘ Disabled"));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to toggle!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildNavItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ── Click handler ──────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(TITLE_NAV) && !title.equals(TITLE_STATUS)
                && !title.equals(TITLE_UPGRADES) && !title.equals(TITLE_MESSAGES)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        int slot = event.getRawSlot();

        switch (title) {
            case TITLE_NAV -> {
                if (slot == NAV_SLOT_STATUS)        openStatus(player, profile, plugin);
                else if (slot == NAV_SLOT_UPGRADES) openUpgrades(player, profile, plugin);
                else if (slot == NAV_SLOT_MESSAGES) openMessages(player, profile, plugin);
                else if (slot == NAV_SLOT_BACK)     PickaxeGUI.open(player, profile, plugin);
            }
            case TITLE_STATUS -> {
                if (slot == STATUS_BACK) openNav(player, profile, plugin);
            }
            case TITLE_UPGRADES -> {
                if (slot == UPGRADE_BACK) { openNav(player, profile, plugin); return; }
                for (int i = 0; i < UPGRADE_SLOTS.length; i++) {
                    if (slot == UPGRADE_SLOTS[i]) { handlePerkUpgrade(player, profile, i); return; }
                }
            }
            case TITLE_MESSAGES -> {
                if (slot == MSG_BACK) { openNav(player, profile, plugin); return; }
                for (int i = 0; i < MSG_SLOTS.length; i++) {
                    if (slot == MSG_SLOTS[i]) { handleMessageToggle(player, profile, i); return; }
                }
            }
        }
    }

    private void handlePerkUpgrade(Player player, PlayerProfile profile, int perkIdx) {
        int nextLevel = profile.getPerkChanceLevel(perkIdx) + 1;
        double cost   = PerkManager.getPerkUpgradeCost(nextLevel);

        if (profile.getGrindingPoints() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough GP! Need "
                + ChatColor.AQUA + NumberFormatter.format(new BigNumber(cost), player.getUniqueId()) + " GP§c.");
            return;
        }

        profile.setGrindingPoints(profile.getGrindingPoints() - cost);
        profile.setPerkChanceLevel(perkIdx, nextLevel);

        player.sendMessage(ChatColor.LIGHT_PURPLE + "[✦] §7"
            + PerkManager.PERK_NAMES[perkIdx] + " chance upgraded to level §d" + nextLevel + "§7!");
        openUpgrades(player, profile, plugin);
    }

    private void handleMessageToggle(Player player, PlayerProfile profile, int perkIdx) {
        boolean newState = !profile.isPerkMessageEnabled(perkIdx);
        profile.setPerkMessageEnabled(perkIdx, newState);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "[✦] §7"
            + PerkManager.PERK_NAMES[perkIdx] + " messages "
            + (newState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GRAY + ".");
        openMessages(player, profile, plugin);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = p.getItemMeta();
        if (m != null) { m.setDisplayName(" "); p.setItemMeta(m); }
        return p;
    }

    private static ItemStack makeBack(String destTitle) {
        ItemStack p = new ItemStack(Material.ARROW);
        ItemMeta m = p.getItemMeta();
        m.setDisplayName("§7« §fBack");
        m.setLore(List.of("§7Return to the previous menu"));
        p.setItemMeta(m);
        return p;
    }
}
