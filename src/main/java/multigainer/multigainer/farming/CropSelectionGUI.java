package multigainer.multigainer.farming;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.tools.ToolGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CropSelectionGUI implements Listener {

    public static final String TITLE = "§fCrop Selection";

    // 36-slot layout (4 rows)
    // Row 0 (0-8):   border panes
    // Row 1 (9-17):  crops 0-8
    // Row 2 (18-26): crops 9-15 at slots 18-24, pane at 25, BACK at 26
    // Row 3 (27-35): border panes
    private static final int SLOT_BACK   = 31;
    private static final long COOLDOWN_MS = 5_000L;

    // Per-player crop-switch cooldown (ms timestamp of last switch)
    private final Map<UUID, Long> switchCooldown = new ConcurrentHashMap<>();

    private final Multigainer plugin;

    public CropSelectionGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);
        ItemStack pane = makePane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        int playerTier = profile.getTier();
        int chosenCrop = profile.getChosenCrop();

        for (int i = 0; i < FarmingManager.CROP_COUNT; i++) {
            int slot     = (i < 9) ? (9 + i) : (18 + i - 9);
            boolean unlocked = playerTier >= FarmingManager.CROP_UNLOCK_TIERS[i];
            inv.setItem(slot, buildCropItem(i, chosenCrop == i, unlocked, playerTier));
        }
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

        int cropIndex = -1;
        if (slot >= 9  && slot <= 17) cropIndex = slot - 9;
        else if (slot >= 18 && slot <= 24) cropIndex = slot - 18 + 9;
        if (cropIndex < 0 || cropIndex >= FarmingManager.CROP_COUNT) return;

        // Locked check
        int unlockTier = FarmingManager.CROP_UNLOCK_TIERS[cropIndex];
        if (profile.getTier() < unlockTier) {
            player.sendMessage("§c§lLocked! §7Reach §eTier " + unlockTier + " §7to unlock "
                + FarmingManager.CROP_NAMES[cropIndex] + "§7.");
            return;
        }

        // Already selected
        if (profile.getChosenCrop() == cropIndex) return;

        // 5-second cooldown check
        long now  = System.currentTimeMillis();
        Long last = switchCooldown.get(player.getUniqueId());
        if (last != null && now - last < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (now - last) + 999) / 1000;
            player.sendMessage("§7Crop switch cooldown: §e" + remaining + "s §7remaining.");
            return;
        }
        switchCooldown.put(player.getUniqueId(), now);

        profile.setChosenCrop(cropIndex);
        sendFakeCrops(player, cropIndex, plugin);
        player.sendMessage("§7Selected " + FarmingManager.CROP_NAMES[cropIndex] + " §7as your crop!");

        // Refresh highlights
        Inventory inv  = event.getInventory();
        int playerTier = profile.getTier();
        for (int i = 0; i < FarmingManager.CROP_COUNT; i++) {
            int guiSlot  = (i < 9) ? (9 + i) : (18 + i - 9);
            boolean unlocked = playerTier >= FarmingManager.CROP_UNLOCK_TIERS[i];
            inv.setItem(guiSlot, buildCropItem(i, i == cropIndex, unlocked, playerTier));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        switchCooldown.remove(event.getPlayer().getUniqueId());
    }

    public static void sendFakeCrops(Player player, int cropIndex, Multigainer plugin) {
        FarmingManager.sendFieldCropChange(player, cropIndex, plugin);
    }

    private static ItemStack buildCropItem(int cropIndex, boolean selected, boolean unlocked, int playerTier) {
        ItemStack item;
        ItemMeta  meta;

        if (!unlocked) {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            meta = item.getItemMeta();
            meta.setDisplayName("§c§l🔒 Locked");
            meta.setLore(List.of(
                "§7Requires§8: §eTier " + FarmingManager.CROP_UNLOCK_TIERS[cropIndex],
                "§7Your Tier§8:  §f" + playerTier,
                "",
                FarmingManager.CROP_NAMES[cropIndex] + " §8(LOCKED)"
            ));
        } else {
            item = new ItemStack(FarmingManager.CROP_DISPLAY_ITEMS[cropIndex]);
            meta = item.getItemMeta();
            meta.setDisplayName(FarmingManager.CROP_NAMES[cropIndex]);

            long   multi    = FarmingManager.getSeedMultiplier(cropIndex);
            String multiStr = "§6×" + FarmingManager.fmtCount(multi);

            List<String> lore = new ArrayList<>();
            lore.add("§7Seed Multiplier§8: " + multiStr + " §7Seeds");
            lore.add("§7Unlock Tier§8: §e" + FarmingManager.CROP_UNLOCK_TIERS[cropIndex]);
            lore.add("");
            lore.add(selected ? "§aCurrently selected!" : "§eClick to select!");
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makePane(Material mat) {
        ItemStack p = new ItemStack(mat);
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
