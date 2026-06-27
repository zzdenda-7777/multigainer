package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.farming.FarmingManager;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.perks.PerkManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ToolItemHandler implements Listener {
    private final Multigainer plugin;

    public static final String PDC_PICKAXE_KEY = "multigainer_pickaxe";
    public static final String PDC_HOE_KEY     = "multigainer_hoe";

    public ToolItemHandler(Multigainer plugin) { this.plugin = plugin; }

    // Build the hoe from a player's live profile (join, tier-up, lore refresh)
    public ItemStack getHoeForProfile(PlayerProfile profile) {
        return getHoeForProfile(profile, null);
    }

    public ItemStack getHoeForProfile(PlayerProfile profile, java.util.UUID uid) {
        int hoeTier  = profile != null ? profile.getHoeTier() : 0;
        Material mat = FarmingManager.HOE_MATERIALS[hoeTier];
        String color = FarmingManager.HOE_TIER_COLORS[hoeTier];
        String name  = FarmingManager.HOE_TIER_NAMES[hoeTier];

        ItemStack hoe  = new ItemStack(mat);
        ItemMeta  meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l" + name.toUpperCase() + " HOE");
            meta.setLore(profile != null ? ToolGUI.buildInventoryHoeLore(profile, uid) : List.of("§7Right-click to open menu!"));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_HOE_KEY), PersistentDataType.BYTE, (byte) 1
            );
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    // Fallback — no profile data
    public ItemStack getCustomHoe() {
        ItemStack hoe  = new ItemStack(Material.WOODEN_HOE);
        ItemMeta  meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f§lWooden Multigainer Hoe ");
            meta.setLore(List.of("§7Right click to open menu!"));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_HOE_KEY), PersistentDataType.BYTE, (byte) 1
            );
            hoe.setItemMeta(meta);
        }
        return hoe;
    }

    // Builds the pickaxe item using the player's current tier and mining speed level
    public ItemStack getPickaxeForProfile(PlayerProfile profile) {
        return getPickaxeForProfile(profile, null);
    }

    public ItemStack getPickaxeForProfile(PlayerProfile profile, java.util.UUID uid) {
        int tier = profile != null ? profile.getPickaxeTier() : 0;
        int speedLevel = profile != null ? profile.getMiningSpeedLevel() : 0;
        int xpLvl = profile != null ? profile.getXpMultiLevel() : 0;
        int gemLvl = profile != null ? profile.getGemMultiLevel() : 0;

        Material mat = PickaxeManager.TIER_MATERIALS[tier];
        String color = PickaxeManager.TIER_COLORS[tier];
        String tierName = PickaxeManager.TIER_NAMES[tier];

        ItemStack pickaxe = new ItemStack(mat);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l" + tierName + " Pickaxe");

            // Compute stats for lore
            long totalBlocks = 0;
            if (profile != null) {
                for (int i = 0; i < 17; i++) totalBlocks += profile.getBlockStorage(i);
            }
            int totalPerks = 0;
            if (profile != null) {
                for (int i = 0; i < PerkManager.PERK_COUNT; i++) totalPerks += profile.getPerkCount(i);
            }
            BigNumber perkMulti = profile != null
                    ? PerkManager.getTotalPerkMultiplierBig(profile.getPerkCounts())
                    : new BigNumber(1.0);

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§8§m──────────────────────");
            lore.add("§7Tier§8: " + color + tierName + " Pickaxe §8[§eTier " + tier + "§8]");
            lore.add("§8§m──────────────────────");
            lore.add("§7Mining Speed§8: §f" + speedLevel + " lvl");
            lore.add("§7XP Multi§8: §a×" + String.format("%.2f", PickaxeManager.getXpMultiplier(xpLvl)));
            lore.add("§7Gem Multi§8: §b×" + String.format("%.2f", PickaxeManager.getGemMultiplier(gemLvl)));
            lore.add("§8§m──────────────────────");
            lore.add("§7Rebirth Points§8: §5"
                    + (profile != null ? NumberFormatter.format(profile.getRebirthPoints(), uid) : "0"));
            lore.add("§7Grinding Points§8: §c"
                    + (profile != null ? NumberFormatter.format(new BigNumber(profile.getGrindingPoints()), uid) : "0"));
            lore.add("§7Blocks Mined§8: §f" + NumberFormatter.format(new BigNumber((double) totalBlocks), uid));
            lore.add("§7Perks§8: §f" + NumberFormatter.format(new BigNumber((double) totalPerks), uid)
                    + " §8(§a" + NumberFormatter.format(perkMulti, uid) + "x §7multi§8)");
            lore.add("§8§m──────────────────────");
            lore.add("§7Right-click to open pickaxe menu!");
            meta.setLore(lore);

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            if (speedLevel > 0) {
                Enchantment efficiency = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("efficiency"));
                if (efficiency != null) meta.addEnchant(efficiency, speedLevel, true);
            }

            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_PICKAXE_KEY), PersistentDataType.BYTE, (byte) 1
            );
            pickaxe.setItemMeta(meta);
        }
        return pickaxe;
    }

    public boolean isCustomPickaxe(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;

        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, PDC_PICKAXE_KEY), PersistentDataType.BYTE);
    }

    public boolean isCustomHoe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, PDC_HOE_KEY), PersistentDataType.BYTE);
    }

    public void updatePickaxeInInventory(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        ItemStack newPickaxe = getPickaxeForProfile(profile, player.getUniqueId());
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isCustomPickaxe(inv.getItem(i))) { inv.setItem(i, newPickaxe); return; }
        }
        inv.setItem(profile != null ? profile.getPickaxeSlot() : 1, newPickaxe);
    }

    public void updateHoeInInventory(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;
        ItemStack newHoe = getHoeForProfile(profile, player.getUniqueId());
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isCustomHoe(inv.getItem(i))) { inv.setItem(i, newHoe); return; }
        }
        inv.setItem(profile.getHoeSlot(), newHoe);
    }

    // Broadcast hoe tier-up if a farm level threshold was crossed
    public void checkHoeTierUp(Player player, PlayerProfile profile, int oldFarmLevel, int newFarmLevel) {
        int[] reqs = FarmingManager.HOE_TIER_REQUIREMENTS;
        for (int tier = reqs.length - 1; tier > 0; tier--) {
            if (oldFarmLevel < reqs[tier] && newFarmLevel >= reqs[tier]) {
                profile.setHoeTier(tier);
                updateHoeInInventory(player);
                String msg = "§e" + player.getName()
                    + " §7has reached a " + FarmingManager.HOE_TIER_COLORS[tier]
                    + "§l" + FarmingManager.HOE_TIER_NAMES[tier] + " Hoe§7!";
                Bukkit.broadcastMessage(msg);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null) return;

        if (isCustomHoe(item)) {
            event.setCancelled(true);
            plugin.getToolGUI().openHoeGUI(event.getPlayer());
        } else if (isCustomPickaxe(item)) {
            event.setCancelled(true);
            PlayerProfile profile = plugin.getPlayerDataManager().getProfile(event.getPlayer().getUniqueId());
            PickaxeGUI.open(event.getPlayer(), profile, plugin);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor  = event.getCursor();
        boolean currentIsCustom = current != null && !current.getType().isAir()
                && (isCustomHoe(current) || isCustomPickaxe(current));
        boolean cursorIsCustom  = cursor  != null && !cursor.getType().isAir()
                && (isCustomHoe(cursor)  || isCustomPickaxe(cursor));
        if (!currentIsCustom && !cursorIsCustom) return;
        // Allow free movement within the player's own inventory; block external inventories
        if (event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isCustomHoe(droppedItem) || isCustomPickaxe(droppedItem)) event.setCancelled(true);
    }
}
