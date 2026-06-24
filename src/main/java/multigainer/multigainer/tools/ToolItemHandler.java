package multigainer.multigainer.tools;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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

import java.util.Collections;

public class ToolItemHandler implements Listener {
    private final Multigainer plugin;

    // PDC key used to identify the custom pickaxe regardless of enchantments or tier
    public static final String PDC_PICKAXE_KEY = "multigainer_pickaxe";
    public static final String PDC_HOE_KEY = "multigainer_hoe";

    private final String hoeName = "§a§lCustom Wooden Hoe §7(Right Click)";

    public ToolItemHandler(Multigainer plugin) {
        this.plugin = plugin;
    }

    public ItemStack getCustomHoe() {
        ItemStack hoe = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(hoeName);
            meta.setLore(Collections.singletonList("§7Right click to view upgrades!"));
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
        int tier = profile != null ? profile.getPickaxeTier() : 0;
        int speedLevel = profile != null ? profile.getMiningSpeedLevel() : 0;

        Material mat = PickaxeManager.TIER_MATERIALS[tier];
        String color = PickaxeManager.TIER_COLORS[tier];
        String tierName = PickaxeManager.TIER_NAMES[tier];

        ItemStack pickaxe = new ItemStack(mat);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "§l" + tierName + " Pickaxe §8(Right Click)");
            meta.setLore(Collections.singletonList("§7Right click to open the pickaxe menu!"));
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
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, PDC_PICKAXE_KEY), PersistentDataType.BYTE);
    }

    public boolean isCustomHoe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, PDC_HOE_KEY), PersistentDataType.BYTE);
    }

    // Updates the pickaxe in the player's inventory slot 1 to reflect current profile state
    public void updatePickaxeInInventory(Player player) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        player.getInventory().setItem(1, getPickaxeForProfile(profile));
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
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem == null) return;
        if (isCustomHoe(currentItem) || isCustomPickaxe(currentItem)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        if (isCustomHoe(droppedItem) || isCustomPickaxe(droppedItem)) {
            event.setCancelled(true);
        }
    }
}