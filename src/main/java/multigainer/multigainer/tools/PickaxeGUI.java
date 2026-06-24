package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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

public class PickaxeGUI implements Listener {

    public static final String TITLE = "§8⛏ §fPickaxe Menu";

    private static final int SLOT_UPGRADES = 11;
    private static final int SLOT_TIER_UP  = 13;
    private static final int SLOT_STORAGE  = 15;

    private final Multigainer plugin;

    public PickaxeGUI(Multigainer plugin) {
        this.plugin = plugin;
    }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Glass pane filler
        ItemStack pane = makePane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        // --- Slot 11: Upgrades ---
        int speedLvl = profile.getMiningSpeedLevel();
        int xpLvl    = profile.getXpMultiLevel();
        int gemLvl   = profile.getGemMultiLevel();

        ItemStack upgrades = new ItemStack(Material.ANVIL);
        ItemMeta um = upgrades.getItemMeta();
        um.setDisplayName("§e§lUpgrades");
        um.setLore(Arrays.asList(
            "§8———————————————",
            "§7Mining Speed  §f" + speedLvl + (speedLvl >= 50 ? " §a(MAX)" : ""),
            "§7XP Multi      §ax" + String.format("%.2f", PickaxeManager.getXpMultiplier(xpLvl)),
            "§7Gem Multi     §bx" + String.format("%.2f", PickaxeManager.getGemMultiplier(gemLvl)),
            "§8———————————————",
            "§eClick to open upgrades!"
        ));
        upgrades.setItemMeta(um);
        inv.setItem(SLOT_UPGRADES, upgrades);

        // --- Slot 13: Tier Up ---
        inv.setItem(SLOT_TIER_UP, buildTierUpItem(profile));

        // --- Slot 15: Block Storage ---
        ItemStack storage = new ItemStack(Material.CHEST);
        ItemMeta sm = storage.getItemMeta();
        sm.setDisplayName("§b§lBlock Storage");
        sm.setLore(Arrays.asList(
            "§8———————————————",
            "§7View how many blocks you have",
            "§7collected from mining.",
            "§8———————————————",
            "§eClick to open!"
        ));
        storage.setItemMeta(sm);
        inv.setItem(SLOT_STORAGE, storage);

        player.openInventory(inv);
    }

    private static ItemStack buildTierUpItem(PlayerProfile profile) {
        int currentTier = profile.getPickaxeTier();
        boolean isMax = currentTier >= PickaxeManager.TIER_NAMES.length - 1;

        if (isMax) {
            ItemStack item = new ItemStack(PickaxeManager.TIER_MATERIALS[currentTier]);
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§a§lMax Tier Reached");
            m.setLore(Arrays.asList(
                "§8———————————————",
                "§7Current Tier: " + PickaxeManager.TIER_COLORS[currentTier] + "§l" + PickaxeManager.TIER_NAMES[currentTier],
                "§8———————————————",
                "§7You have reached the maximum",
                "§7pickaxe tier. Well done!"
            ));
            item.setItemMeta(m);
            return item;
        }

        int nextTier = currentTier + 1;
        int[] reqs = PickaxeManager.TIER_UP_REQUIREMENTS[currentTier];
        int reqTier = reqs[0];
        int reqMineLevel = reqs[1];

        boolean tierMet  = profile.getTier() >= reqTier;
        boolean levelMet = profile.getMiningLevel() >= reqMineLevel;
        boolean canUpgrade = tierMet && levelMet;

        Material mat = canUpgrade ? PickaxeManager.TIER_MATERIALS[nextTier] : Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();

        m.setDisplayName((canUpgrade ? "§a§l" : "§c§l") + "Tier Up  "
            + PickaxeManager.TIER_COLORS[currentTier] + PickaxeManager.TIER_NAMES[currentTier]
            + " §8→ "
            + PickaxeManager.TIER_COLORS[nextTier] + PickaxeManager.TIER_NAMES[nextTier]);

        List<String> lore = new ArrayList<>();
        lore.add("§8———————————————");
        lore.add("§7Current: " + PickaxeManager.TIER_COLORS[currentTier] + "§l" + PickaxeManager.TIER_NAMES[currentTier] + " Pickaxe");
        lore.add("§7Next:    " + PickaxeManager.TIER_COLORS[nextTier] + "§l" + PickaxeManager.TIER_NAMES[nextTier] + " Pickaxe");
        lore.add("§8———————————————");
        lore.add((tierMet  ? "§a✔" : "§c✘") + " §7Tier:         §f" + reqTier  + " §8(yours: " + profile.getTier() + ")");
        lore.add((levelMet ? "§a✔" : "§c✘") + " §7Mining Level: §f" + formatLevel(reqMineLevel) + " §8(yours: " + formatLevel(profile.getMiningLevel()) + ")");
        lore.add("§8———————————————");
        lore.add(canUpgrade ? "§eClick to upgrade!" : "§cRequirements not met.");
        m.setLore(lore);
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

        if (slot == SLOT_UPGRADES) {
            PickaxeUpgradeGUI.open(player, profile, plugin);

        } else if (slot == SLOT_TIER_UP) {
            handleTierUp(player, profile);

        } else if (slot == SLOT_STORAGE) {
            PickaxeBlockStorageGUI.open(player, profile, plugin);
        }
    }

    private void handleTierUp(Player player, PlayerProfile profile) {
        int currentTier = profile.getPickaxeTier();
        if (currentTier >= PickaxeManager.TIER_NAMES.length - 1) return;

        int[] reqs = PickaxeManager.TIER_UP_REQUIREMENTS[currentTier];
        if (profile.getTier() < reqs[0] || profile.getMiningLevel() < reqs[1]) return;

        profile.setPickaxeTier(currentTier + 1);
        plugin.getToolHandler().updatePickaxeInInventory(player);

        player.sendMessage("§8[§b⛏§8] §7Your pickaxe has been upgraded to "
            + PickaxeManager.TIER_COLORS[currentTier + 1] + "§l" + PickaxeManager.TIER_NAMES[currentTier + 1]
            + " §7tier!");

        // Reopen to reflect new tier
        open(player, profile, plugin);
    }

    private static ItemStack makePane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta m = pane.getItemMeta();
        m.setDisplayName(" ");
        pane.setItemMeta(m);
        return pane;
    }

    private static String formatLevel(int level) {
        if (level >= 1_000_000) return String.format("%.1fM", level / 1_000_000.0);
        if (level >= 1_000)    return String.format("%.1fK", level / 1_000.0);
        return String.valueOf(level);
    }
}