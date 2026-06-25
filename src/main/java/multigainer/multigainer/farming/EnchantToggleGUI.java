package multigainer.multigainer.farming;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.ToolGUI;
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

public class EnchantToggleGUI implements Listener {

    public static final String TITLE = "§fEnchant Messages";

    // 27-slot layout
    // Row 0 (0-8):   border panes
    // Row 1 (9-17):  [P][TNT][P][NUKE][P][WE][P][UD][P]
    // Row 2 (18-26): panes, BACK at slot 26
    private static final int[] ENCHANT_SLOTS = { 10, 12, 14, 16 };
    private static final int   SLOT_BACK     = 26;

    private final Multigainer plugin;

    public EnchantToggleGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        ItemStack pane = makePane();
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        int farmLevel = profile.getFarmingLevel();
        for (int i = 0; i < 4; i++) {
            inv.setItem(ENCHANT_SLOTS[i], buildEnchantItem(i, profile.isEnchantMessageEnabled(i), farmLevel));
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
        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile == null) return;

        if (slot == SLOT_BACK) {
            ToolGUI.open(player, profile, plugin);
            return;
        }

        for (int i = 0; i < ENCHANT_SLOTS.length; i++) {
            if (slot == ENCHANT_SLOTS[i]) {
                boolean newState = !profile.isEnchantMessageEnabled(i);
                profile.setEnchantMessageEnabled(i, newState);
                event.getInventory().setItem(slot, buildEnchantItem(i, newState, profile.getFarmingLevel()));
                player.sendMessage("§8[§e⚡§8] §7" + FarmingManager.ENCHANT_NAMES[i]
                    + " messages " + (newState ? "§aenabled" : "§cdisabled") + "§7.");
                return;
            }
        }
    }

    private static ItemStack buildEnchantItem(int idx, boolean enabled, int farmLevel) {
        Material mat  = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();

        String[] icons   = { "💥", "💣", "🌍", "🌌" };
        meta.setDisplayName((enabled ? "§a§l" : "§7§l") + icons[idx] + " " + FarmingManager.ENCHANT_NAMES[idx]);

        double  chance   = FarmingManager.getEnchantChance(idx, farmLevel);
        String  seedAmt  = NumberFormatter.format(new BigNumber((double) FarmingManager.ENCHANT_SEED_MULTI[idx]));
        String[] maxLvls = { "2,500", "25,000", "500,000", "25,000,000" };
        meta.setLore(Arrays.asList(
            "§7Grants §c" + seedAmt + "x Seeds §7on proc.",
            "§7Current Chance §8» " + FarmingManager.formatChance(chance),
            "§7Max Chance §8»     §a25% §8(Lvl §f" + maxLvls[idx] + "§8)",
            "",
            "§7Chat Message §8» " + (enabled ? "§a✔ Enabled" : "§c✘ Disabled"),
            "",
            "§eClick to toggle!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
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
