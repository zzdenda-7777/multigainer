package multigainer.multigainer.artifacts;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.ToolGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArtifactGUI implements Listener {

    private static final String TITLE    = "§5§lArtifacts";
    private static final int    SIZE      = 36;
    private static final int    SLOT_1    = 11; // free
    private static final int    SLOT_2    = 13; // 150 rubies
    private static final int    SLOT_3    = 15; // 500 rubies
    private static final int    SLOT_VAULT = 22; // artifact vault
    private static final int    SLOT_BACK  = 31; // back to hoe GUI
    private static final double COST_2    = 150;
    private static final double COST_3    = 500;

    private final Multigainer plugin;

    public ArtifactGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));

        ItemStack glass = makeGlass();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);

        inv.setItem(SLOT_1, buildSlotDisplay(plugin, profile, 0));
        inv.setItem(SLOT_2, buildSlotDisplay(plugin, profile, 1));
        inv.setItem(SLOT_3, buildSlotDisplay(plugin, profile, 2));
        inv.setItem(SLOT_VAULT, makeVaultButton());
        inv.setItem(SLOT_BACK, makeBack());

        player.openInventory(inv);
    }

    private static ItemStack buildSlotDisplay(Multigainer plugin, PlayerProfile profile, int idx) {
        if (idx == 0 || profile.isArtifactSlotUnlocked(idx)) {
            String id = profile.getArtifactSlot(idx);
            if (id != null && !id.isEmpty()) {
                ArtifactManager.ArtifactRecord r = ArtifactManager.getById(id);
                if (r != null) return ArtifactManager.buildItem(plugin, r);
            }
            return makeEmptySlot(idx == 0);
        }
        double cost = idx == 1 ? COST_2 : COST_3;
        return makeLockedSlot(cost);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isArtifactTitle(event)) return;

        // Allow free movement in player's own inventory section
        if (event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory) return;

        event.setCancelled(true);

        int slot = event.getSlot();

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (slot == SLOT_BACK) {
            Bukkit.getScheduler().runTask(plugin, () -> ToolGUI.open(player, profile, plugin));
            return;
        }
        if (slot == SLOT_VAULT) {
            Bukkit.getScheduler().runTask(plugin, () -> ArtifactStorageGUI.open(player, profile, plugin));
            return;
        }
        if (slot != SLOT_1 && slot != SLOT_2 && slot != SLOT_3) return;

        int slotIdx = slot == SLOT_1 ? 0 : slot == SLOT_2 ? 1 : 2;
        handleSlotClick(event, player, profile, slotIdx);
    }

    private void handleSlotClick(InventoryClickEvent event, Player player, PlayerProfile profile, int slotIdx) {
        // Locked slot → try to unlock
        if (slotIdx > 0 && !profile.isArtifactSlotUnlocked(slotIdx)) {
            double cost = slotIdx == 1 ? COST_2 : COST_3;
            if (profile.getRubies().toDouble() < cost) {
                player.sendMessage("§cYou need §e" + (int) cost + " §7Rubies §cto unlock this slot!");
                return;
            }
            profile.setRubies(profile.getRubies().subtract(new BigNumber(cost)));
            profile.setArtifactSlotUnlocked(slotIdx, true);
            player.sendMessage("§a§l✦ §aSlot " + (slotIdx + 1) + " unlocked!");
            Bukkit.getScheduler().runTask(plugin, () -> open(player, profile, plugin));
            return;
        }

        ItemStack cursor = player.getItemOnCursor();
        String currentId = profile.getArtifactSlot(slotIdx);

        // Player is holding an artifact → try to place
        if (cursor != null && !cursor.getType().isAir() && ArtifactManager.isArtifact(plugin, cursor)) {
            ArtifactManager.ArtifactRecord incoming = ArtifactManager.getFromItem(plugin, cursor);
            if (incoming == null) return;

            // Block duplicate type across slots
            for (int i = 0; i < 3; i++) {
                if (i == slotIdx) continue;
                String otherId = profile.getArtifactSlot(i);
                if (otherId == null || otherId.isEmpty()) continue;
                ArtifactManager.ArtifactRecord other = ArtifactManager.getById(otherId);
                if (other != null && other.type() == incoming.type()) {
                    player.sendMessage("§cYou already have a §e" + incoming.type().displayName
                            + " §cartifact equipped! Remove it first.");
                    return;
                }
            }

            // Swap: if slot has something, give it back on cursor
            if (currentId != null && !currentId.isEmpty()) {
                ArtifactManager.ArtifactRecord existing = ArtifactManager.getById(currentId);
                if (existing != null) {
                    ItemStack existingItem = ArtifactManager.buildItem(plugin, existing);
                    player.setItemOnCursor(existingItem);
                } else {
                    player.setItemOnCursor(null);
                }
            } else {
                player.setItemOnCursor(null);
            }

            profile.setArtifactSlot(slotIdx, incoming.id());
            Bukkit.getScheduler().runTask(plugin, () -> open(player, profile, plugin));
            return;
        }

        // Empty cursor + slot has artifact → remove
        if ((cursor == null || cursor.getType().isAir()) && currentId != null && !currentId.isEmpty()) {
            ArtifactManager.ArtifactRecord r = ArtifactManager.getById(currentId);
            if (r == null) return;
            player.setItemOnCursor(ArtifactManager.buildItem(plugin, r));
            profile.setArtifactSlot(slotIdx, "");
            Bukkit.getScheduler().runTask(plugin, () -> open(player, profile, plugin));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isArtifactTitle(event.getView().title())) return;

        // Return any artifact left on cursor to the player's inventory
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir() && ArtifactManager.isArtifact(plugin, cursor)) {
            player.setItemOnCursor(null);
            player.getInventory().addItem(cursor).forEach(
                    (k, v) -> player.getWorld().dropItemNaturally(player.getLocation(), v));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean isArtifactTitle(InventoryClickEvent e) {
        return isArtifactTitle(e.getView().title());
    }

    private boolean isArtifactTitle(net.kyori.adventure.text.Component title) {
        return TITLE.equals(LegacyComponentSerializer.legacySection().serialize(title));
    }

    private static ItemStack makeVaultButton() {
        ItemStack item = new ItemStack(org.bukkit.Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§lArtifact Vault");
            meta.setLore(List.of(
                "§8§m──────────────────────",
                "§7Store your artifact collection",
                "§7safely in your personal vault.",
                "§8§m──────────────────────",
                "§eClick to open!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeBack() {
        ItemStack p = new ItemStack(org.bukkit.Material.ARROW);
        ItemMeta m = p.getItemMeta();
        if (m != null) {
            m.setDisplayName("§7« §fBack");
            m.setLore(List.of("§7Return to the Hoe Menu"));
            p.setItemMeta(m);
        }
        return p;
    }

    private static ItemStack makeGlass() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        if (m != null) { m.setDisplayName("§8"); g.setItemMeta(m); }
        return g;
    }

    private static ItemStack makeEmptySlot(boolean isFree) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a§lEmpty Artifact Slot");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m──────────────────────");
            lore.add("§7Hold an artifact and click");
            lore.add("§7to equip it here.");
            if (isFree) lore.add("§a✦ Free Slot");
            lore.add("§8§m──────────────────────");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack makeLockedSlot(double cost) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lLocked Slot");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m──────────────────────");
            lore.add("§7Cost: §e" + NumberFormatter.format(new BigNumber(cost)) + " §7Rubies");
            lore.add("§7Click to unlock.");
            lore.add("§8§m──────────────────────");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
