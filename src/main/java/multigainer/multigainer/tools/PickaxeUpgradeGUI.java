package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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

import java.util.Arrays;
import java.util.List;

public class PickaxeUpgradeGUI implements Listener {

    public static final String TITLE = "§8⚒ §fPickaxe Upgrades";

    private static final int SLOT_SPEED   = 10;
    private static final int SLOT_XP      = 13;
    private static final int SLOT_GEM     = 16;
    private static final int SLOT_BACK    = 22;

    private final Multigainer plugin;

    public PickaxeUpgradeGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        ItemStack pane = makePane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(SLOT_SPEED, buildMiningSpeedItem(profile));
        inv.setItem(SLOT_XP,    buildXpMultiItem(profile));
        inv.setItem(SLOT_GEM,   buildGemMultiItem(profile));
        inv.setItem(SLOT_BACK,  buildBackItem());

        player.openInventory(inv);
    }

    // --- Item Builders ---

    private static ItemStack buildMiningSpeedItem(PlayerProfile profile) {
        int level = profile.getMiningSpeedLevel();
        boolean isMax = level >= 50;
        BigNumber cost = PickaxeManager.getMiningSpeedCost(level);

        ItemStack item = new ItemStack(Material.WIND_CHARGE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§f§lMining Speed" + (isMax ? " §8(MAX)" : ""));
        m.setLore(Arrays.asList(
            "§8———————————————",
            "§7Boosts your mining speed by",
            "§7increasing your Efficiency level.",
            "§8———————————————",
            "§7Level:      §f" + level + " §8/ §750",
            "§7Efficiency: §fLevel " + level,
            "§8———————————————",
            isMax ? "§a✔ Max level reached!" : "§7Cost: §b" + NumberFormatter.format(cost) + " Gems",
            isMax ? "" : "§8Next Level: §fLevel " + (level + 1),
            "§8———————————————",
            isMax ? "§7No further upgrades available." : "§eClick to upgrade!"
        ));
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack buildXpMultiItem(PlayerProfile profile) {
        int level = profile.getXpMultiLevel();
        double totalMulti = PickaxeManager.getXpMultiplier(level);
        BigNumber cost = PickaxeManager.getXpMultiCost(level);

        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§a§lXP Multiplier");
        m.setLore(Arrays.asList(
            "§8———————————————",
            "§7Multiplies all mining XP gained.",
            "§7Stacks with block XP values.",
            "§8———————————————",
            "§7Level:     §a" + level,
            "§7Total XP Multi: §ax" + String.format("%.4f", totalMulti),
            "§8———————————————",
            "§7Cost: §b" + NumberFormatter.format(cost) + " Gems",
            "§7Per Level: §a+1.05x §8(exponential)",
            "§8———————————————",
            "§eClick to upgrade!"
        ));
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack buildGemMultiItem(PlayerProfile profile) {
        int level = profile.getGemMultiLevel();
        int mineLevel = profile.getMiningLevel();
        double upgradeMulti = PickaxeManager.getGemMultiplier(level);
        double miningLevelMulti = Math.pow(1.02, mineLevel);
        double totalMulti = upgradeMulti * miningLevelMulti;
        BigNumber cost = PickaxeManager.getGemMultiCost(level);

        ItemStack item = new ItemStack(Material.LIGHT_BLUE_DYE);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§b§lGem Multiplier");
        m.setLore(Arrays.asList(
            "§8———————————————",
            "§7Multiplies all gems gained.",
            "§7Stacks with your Mining Level multi.",
            "§8———————————————",
            "§7Level:          §b" + level,
            "§7Upgrade Multi:  §bx" + String.format("%.4f", upgradeMulti),
            "§7Mining Lvl Multi: §bx" + String.format("%.4f", miningLevelMulti),
            "§7Total Gem Multi: §bx" + String.format("%.4f", totalMulti),
            "§8———————————————",
            "§7Cost: §b" + NumberFormatter.format(cost) + " Gems",
            "§7Per Level: §b+1.03x §8(exponential)",
            "§8———————————————",
            "§eClick to upgrade!"
        ));
        item.setItemMeta(m);
        return item;
    }

    private static ItemStack buildBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta m = item.getItemMeta();
        m.setDisplayName("§7← Back");
        m.setLore(List.of("§8Return to Pickaxe Menu"));
        item.setItemMeta(m);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (slot == SLOT_BACK) {
            PickaxeGUI.open(player, profile, plugin);
            return;
        }

        if (slot == SLOT_SPEED) {
            purchaseMiningSpeed(player, profile);
        } else if (slot == SLOT_XP) {
            purchaseXpMulti(player, profile);
        } else if (slot == SLOT_GEM) {
            purchaseGemMulti(player, profile);
        }
    }

    private void purchaseMiningSpeed(Player player, PlayerProfile profile) {
        if (profile.getMiningSpeedLevel() >= 50) {
            player.sendMessage("§c§lMax Level! §7Mining Speed is already at level 50.");
            return;
        }
        BigNumber cost = PickaxeManager.getMiningSpeedCost(profile.getMiningSpeedLevel());
        if (profile.getGems().compareTo(cost) < 0) {
            player.sendMessage("§c§lNot enough gems! §7You need §b" + NumberFormatter.format(cost) + " Gems§7.");
            return;
        }
        profile.setGems(profile.getGems().subtract(cost));
        profile.setMiningSpeedLevel(profile.getMiningSpeedLevel() + 1);

        // Apply new efficiency enchant to the held pickaxe
        plugin.getToolHandler().updatePickaxeInInventory(player);
        updateScoreboard(player, profile);

        player.sendMessage("§8[§f⚡§8] §7Mining Speed upgraded to §flevel "
            + profile.getMiningSpeedLevel() + "§7!");
        open(player, profile, plugin);
    }

    private void purchaseXpMulti(Player player, PlayerProfile profile) {
        BigNumber cost = PickaxeManager.getXpMultiCost(profile.getXpMultiLevel());
        if (profile.getGems().compareTo(cost) < 0) {
            player.sendMessage("§c§lNot enough gems! §7You need §b" + NumberFormatter.format(cost) + " Gems§7.");
            return;
        }
        profile.setGems(profile.getGems().subtract(cost));
        profile.setXpMultiLevel(profile.getXpMultiLevel() + 1);
        updateScoreboard(player, profile);

        player.sendMessage("§8[§a⭐§8] §7XP Multiplier upgraded to §alevel "
            + profile.getXpMultiLevel() + " §8(§ax"
            + String.format("%.4f", PickaxeManager.getXpMultiplier(profile.getXpMultiLevel())) + "§8)§7!");
        open(player, profile, plugin);
    }

    private void purchaseGemMulti(Player player, PlayerProfile profile) {
        BigNumber cost = PickaxeManager.getGemMultiCost(profile.getGemMultiLevel());
        if (profile.getGems().compareTo(cost) < 0) {
            player.sendMessage("§c§lNot enough gems! §7You need §b" + NumberFormatter.format(cost) + " Gems§7.");
            return;
        }
        profile.setGems(profile.getGems().subtract(cost));
        profile.setGemMultiLevel(profile.getGemMultiLevel() + 1);
        updateScoreboard(player, profile);

        player.sendMessage("§8[§b✦§8] §7Gem Multiplier upgraded to §blevel "
            + profile.getGemMultiLevel() + " §8(§bx"
            + String.format("%.4f", PickaxeManager.getGemMultiplier(profile.getGemMultiLevel())) + "§8)§7!");
        open(player, profile, plugin);
    }

    private void updateScoreboard(Player player, PlayerProfile profile) {
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().updateScoreboard(
                player, profile.getMoney(), profile.getGems(), profile.getRubies(),
                profile.getFarmingLevel(), profile.getFarmingXp(),
                profile.getMiningLevel(), profile.getMiningXp()
            );
        }
    }

    private static ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);
        return pane;
    }
}