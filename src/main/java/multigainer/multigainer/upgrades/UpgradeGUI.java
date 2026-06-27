package multigainer.multigainer.upgrades;

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

public class UpgradeGUI implements Listener {
    private final Multigainer plugin;
    private static final String TITLE = "§8✦ §fUpgrades §8✦";

    public UpgradeGUI(Multigainer plugin) { this.plugin = plugin; }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());

        // Fill with panes
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        if (pm != null) { pm.setDisplayName(" "); pane.setItemMeta(pm); }
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        java.util.UUID uid = player.getUniqueId();
        inv.setItem(11, buildMoneyItem(profile, uid));
        inv.setItem(13, buildGemItem(profile, uid));
        inv.setItem(15, buildFarmItem(profile, uid));
        player.openInventory(inv);
    }

    private ItemStack buildMoneyItem(PlayerProfile profile, java.util.UUID uid) {
        int lvl = profile.getUpgradeLevel();
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Money Multi");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.GOLD + lvl + "/" + UpgradeManager.MONEY_MAX_LEVEL);
        lore.add(ChatColor.GRAY + "Total Multi: " + ChatColor.GOLD + NumberFormatter.format(UpgradeManager.getMoneyTotalMultiplier(lvl), uid) + "x");
        lore.add(" ");
        if (lvl < UpgradeManager.MONEY_MAX_LEVEL) {
            int next = lvl + 1;
            lore.add(ChatColor.GRAY + "Next Level Multi: " + ChatColor.AQUA + String.format("%.2fx", UpgradeManager.getMoneyTierMultiplier(next)));
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + NumberFormatter.format(UpgradeManager.getMoneyUpgradeCost(next), uid));
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        } else {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "MAX LEVEL");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildGemItem(PlayerProfile profile, java.util.UUID uid) {
        int lvl = profile.getGemUpgradeLevel();
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Gem Multi");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + lvl + "/" + UpgradeManager.GEM_MAX_LEVEL);
        lore.add(ChatColor.GRAY + "Total Multi: " + ChatColor.GREEN + NumberFormatter.format(UpgradeManager.getGemTotalMultiplier(lvl), uid) + "x");
        lore.add(" ");
        if (lvl < UpgradeManager.GEM_MAX_LEVEL) {
            int next = lvl + 1;
            lore.add(ChatColor.GRAY + "Next Level Multi: " + ChatColor.AQUA + NumberFormatter.format(UpgradeManager.getGemTotalMultiplier(next), uid) + "x");
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + NumberFormatter.format(UpgradeManager.getGemUpgradeCost(next), uid));
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        } else {
            lore.add(ChatColor.RED + "" + ChatColor.BOLD + "MAX LEVEL");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFarmItem(PlayerProfile profile, java.util.UUID uid) {
        int lvl = profile.getFarmMultiUpgradeLevel();
        ItemStack item = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Farm Multi");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + lvl + "/∞");
        lore.add(ChatColor.GRAY + "Total Multi: " + ChatColor.YELLOW + NumberFormatter.format(UpgradeManager.getFarmTotalMultiplier(lvl), uid) + "x");
        lore.add(" ");
        int next = lvl + 1;
        lore.add(ChatColor.GRAY + "Next Level Multi: " + ChatColor.AQUA + NumberFormatter.format(UpgradeManager.getFarmTotalMultiplier(next), uid) + "x");
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "$" + NumberFormatter.format(UpgradeManager.getFarmUpgradeCost(next), uid));
        lore.add(" ");
        lore.add(ChatColor.YELLOW + "Click to upgrade!");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void refreshScoreboard(Player player, PlayerProfile profile) {
        if (plugin.getScoreboardManager() != null)
            plugin.getScoreboardManager().updateScoreboard(player,
                    profile.getMoney(), profile.getGems(), profile.getRubies(),
                    profile.getFarmingLevel(), profile.getFarmingXp(),
                    profile.getMiningLevel(), profile.getMiningXp());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        boolean right = event.isRightClick();
        int slot = event.getSlot();
        if (slot == 11) { if (right) handleMoneyUpgradeMax(player, profile); else handleMoneyUpgrade(player, profile); }
        else if (slot == 13) { if (right) handleGemUpgradeMax(player, profile); else handleGemUpgrade(player, profile); }
        else if (slot == 15) { if (right) handleFarmUpgradeMax(player, profile); else handleFarmUpgrade(player, profile); }
    }

    // ── Single-level upgrades (left click) ───────────────────────────────────

    private void handleMoneyUpgrade(Player player, PlayerProfile profile) {
        int lvl = profile.getUpgradeLevel();
        if (lvl >= UpgradeManager.MONEY_MAX_LEVEL) {
            player.sendMessage(ChatColor.RED + "Money Multi is already at max level!");
            return;
        }
        BigNumber cost = UpgradeManager.getMoneyUpgradeCost(lvl + 1);
        if (profile.getMoney().compareTo(cost) < 0) {
            player.sendMessage(ChatColor.RED + "Need $" + NumberFormatter.format(cost, player.getUniqueId()));
            return;
        }
        profile.setMoney(profile.getMoney().subtract(cost));
        profile.setUpgradeLevel(lvl + 1);
        player.sendMessage(ChatColor.GREEN + "Money Multi upgraded to level " + (lvl + 1) + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }

    private void handleGemUpgrade(Player player, PlayerProfile profile) {
        int lvl = profile.getGemUpgradeLevel();
        if (lvl >= UpgradeManager.GEM_MAX_LEVEL) {
            player.sendMessage(ChatColor.RED + "Gem Multi is already at max level!");
            return;
        }
        BigNumber cost = UpgradeManager.getGemUpgradeCost(lvl + 1);
        if (profile.getMoney().compareTo(cost) < 0) {
            player.sendMessage(ChatColor.RED + "Need $" + NumberFormatter.format(cost, player.getUniqueId()));
            return;
        }
        profile.setMoney(profile.getMoney().subtract(cost));
        profile.setGemUpgradeLevel(lvl + 1);
        player.sendMessage(ChatColor.GREEN + "Gem Multi upgraded to level " + (lvl + 1) + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }

    private void handleFarmUpgrade(Player player, PlayerProfile profile) {
        int lvl = profile.getFarmMultiUpgradeLevel();
        BigNumber cost = UpgradeManager.getFarmUpgradeCost(lvl + 1);
        if (profile.getMoney().compareTo(cost) < 0) {
            player.sendMessage(ChatColor.RED + "Need $" + NumberFormatter.format(cost, player.getUniqueId()));
            return;
        }
        profile.setMoney(profile.getMoney().subtract(cost));
        profile.setFarmMultiUpgradeLevel(lvl + 1);
        player.sendMessage(ChatColor.GREEN + "Farm Multi upgraded to level " + (lvl + 1) + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }

    // ── Max upgrades (right click) ────────────────────────────────────────────

    private void handleMoneyUpgradeMax(Player player, PlayerProfile profile) {
        int lvl = profile.getUpgradeLevel();
        if (lvl >= UpgradeManager.MONEY_MAX_LEVEL) {
            player.sendMessage(ChatColor.RED + "Money Multi is already at max level!");
            return;
        }
        int gained = 0;
        while (lvl < UpgradeManager.MONEY_MAX_LEVEL) {
            BigNumber cost = UpgradeManager.getMoneyUpgradeCost(lvl + 1);
            if (profile.getMoney().compareTo(cost) < 0) break;
            profile.setMoney(profile.getMoney().subtract(cost));
            lvl++;
            gained++;
        }
        if (gained == 0) { player.sendMessage(ChatColor.RED + "Can't afford the next level!"); return; }
        profile.setUpgradeLevel(lvl);
        player.sendMessage(ChatColor.GREEN + "Money Multi bulk upgraded §8(§f+" + gained + " levels§8) §ato level " + lvl + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }

    private void handleGemUpgradeMax(Player player, PlayerProfile profile) {
        int lvl = profile.getGemUpgradeLevel();
        if (lvl >= UpgradeManager.GEM_MAX_LEVEL) {
            player.sendMessage(ChatColor.RED + "Gem Multi is already at max level!");
            return;
        }
        int gained = 0;
        while (lvl < UpgradeManager.GEM_MAX_LEVEL) {
            BigNumber cost = UpgradeManager.getGemUpgradeCost(lvl + 1);
            if (profile.getMoney().compareTo(cost) < 0) break;
            profile.setMoney(profile.getMoney().subtract(cost));
            lvl++;
            gained++;
        }
        if (gained == 0) { player.sendMessage(ChatColor.RED + "Can't afford the next level!"); return; }
        profile.setGemUpgradeLevel(lvl);
        player.sendMessage(ChatColor.GREEN + "Gem Multi bulk upgraded §8(§f+" + gained + " levels§8) §ato level " + lvl + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }

    private void handleFarmUpgradeMax(Player player, PlayerProfile profile) {
        int lvl = profile.getFarmMultiUpgradeLevel();
        int gained = 0;
        while (true) {
            BigNumber cost = UpgradeManager.getFarmUpgradeCost(lvl + 1);
            if (profile.getMoney().compareTo(cost) < 0) break;
            profile.setMoney(profile.getMoney().subtract(cost));
            lvl++;
            gained++;
        }
        if (gained == 0) { player.sendMessage(ChatColor.RED + "Can't afford the next level!"); return; }
        profile.setFarmMultiUpgradeLevel(lvl);
        player.sendMessage(ChatColor.GREEN + "Farm Multi bulk upgraded §8(§f+" + gained + " levels§8) §ato level " + lvl + "!");
        refreshScoreboard(player, profile);
        openGUI(player);
    }
}