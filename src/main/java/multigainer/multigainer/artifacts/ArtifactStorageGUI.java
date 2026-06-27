package multigainer.multigainer.artifacts;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
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

import java.util.List;

public class ArtifactStorageGUI implements Listener {

    public static final String TITLE = "§5§lArtifact Vault";
    private static final int SIZE        = 54;
    private static final int VAULT_SLOTS = 45; // rows 0-4
    private static final int SLOT_BACK   = 49;

    private final Multigainer plugin;

    public ArtifactStorageGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE,
                LegacyComponentSerializer.legacySection().deserialize(TITLE));

        // Bottom row border
        ItemStack border = makeBorder();
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Storage area (slots 0-44)
        for (int i = 0; i < VAULT_SLOTS; i++) {
            String id = profile.getArtifactVaultSlot(i);
            if (id != null && !id.isEmpty()) {
                ArtifactManager.ArtifactRecord r = ArtifactManager.getById(id);
                inv.setItem(i, r != null ? ArtifactManager.buildItem(plugin, r) : makeEmpty());
            } else {
                inv.setItem(i, makeEmpty());
            }
        }

        inv.setItem(SLOT_BACK, makeBack());
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isTitle(event.getView().title())) return;

        // Player inventory: allow shift-click to deposit artifacts
        if (event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack cur = event.getCurrentItem();
                if (cur != null && !cur.getType().isAir() && ArtifactManager.isArtifact(plugin, cur)) {
                    depositToVault(player, event, cur);
                }
            }
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (slot == SLOT_BACK) {
            Bukkit.getScheduler().runTask(plugin, () -> ArtifactGUI.open(player, profile, plugin));
            return;
        }

        if (slot >= VAULT_SLOTS) return; // other bottom-row panes

        ItemStack cursor  = player.getItemOnCursor();
        String currentId  = profile.getArtifactVaultSlot(slot);

        // Place artifact from cursor
        if (cursor != null && !cursor.getType().isAir()) {
            if (!ArtifactManager.isArtifact(plugin, cursor)) {
                player.sendMessage("§cOnly artifact items can be stored here!");
                return;
            }
            ArtifactManager.ArtifactRecord incoming = ArtifactManager.getFromItem(plugin, cursor);
            if (incoming == null) return;

            // Swap if slot occupied
            if (currentId != null && !currentId.isEmpty()) {
                ArtifactManager.ArtifactRecord existing = ArtifactManager.getById(currentId);
                player.setItemOnCursor(existing != null ? ArtifactManager.buildItem(plugin, existing) : null);
            } else {
                player.setItemOnCursor(null);
            }
            profile.setArtifactVaultSlot(slot, incoming.id());
            event.getInventory().setItem(slot, ArtifactManager.buildItem(plugin, incoming));
            return;
        }

        // Take artifact from slot to cursor
        if (currentId != null && !currentId.isEmpty()) {
            ArtifactManager.ArtifactRecord r = ArtifactManager.getById(currentId);
            if (r == null) return;
            player.setItemOnCursor(ArtifactManager.buildItem(plugin, r));
            profile.setArtifactVaultSlot(slot, "");
            event.getInventory().setItem(slot, makeEmpty());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isTitle(event.getView().title())) return;
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir() && ArtifactManager.isArtifact(plugin, cursor)) {
            player.setItemOnCursor(null);
            player.getInventory().addItem(cursor).forEach(
                    (k, v) -> player.getWorld().dropItemNaturally(player.getLocation(), v));
        }
    }

    private void depositToVault(Player player, InventoryClickEvent event, ItemStack item) {
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;
        ArtifactManager.ArtifactRecord r = ArtifactManager.getFromItem(plugin, item);
        if (r == null) return;
        for (int i = 0; i < VAULT_SLOTS; i++) {
            String id = profile.getArtifactVaultSlot(i);
            if (id == null || id.isEmpty()) {
                profile.setArtifactVaultSlot(i, r.id());
                event.getClickedInventory().setItem(event.getSlot(), null);
                event.getInventory().setItem(i, ArtifactManager.buildItem(plugin, r));
                return;
            }
        }
        player.sendMessage("§cArtifact Vault is full!");
    }

    private boolean isTitle(net.kyori.adventure.text.Component title) {
        return TITLE.equals(LegacyComponentSerializer.legacySection().serialize(title));
    }

    private static ItemStack makeEmpty() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = g.getItemMeta();
        if (m != null) { m.setDisplayName("§7Empty Slot"); g.setItemMeta(m); }
        return g;
    }

    private static ItemStack makeBorder() {
        ItemStack p = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta m = p.getItemMeta();
        if (m != null) { m.setDisplayName("§8"); p.setItemMeta(m); }
        return p;
    }

    private static ItemStack makeBack() {
        ItemStack p = new ItemStack(Material.ARROW);
        ItemMeta m = p.getItemMeta();
        if (m != null) {
            m.setDisplayName("§7« §fBack");
            m.setLore(List.of("§7Return to Artifacts menu"));
            p.setItemMeta(m);
        }
        return p;
    }
}
