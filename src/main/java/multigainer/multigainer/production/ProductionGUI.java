package multigainer.multigainer.production;

import multigainer.multigainer.Multigainer;
import multigainer.multigainer.data.PlayerProfile;
import multigainer.multigainer.formatting.NumberFormatter;
import multigainer.multigainer.math.BigNumber;
import multigainer.multigainer.tools.PickaxeBlockStorageGUI;
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

public class ProductionGUI implements Listener {

    public static final String TITLE     = "§8⚙ §7Production";
    private static final int   SIZE      = 27;
    private static final int   SLOT_INFO   = 4;
    private static final int   SLOT_WORKER = 11;
    private static final int   SLOT_ENERGY = 15;
    private static final int   SLOT_BACK   = 22;

    private final Multigainer plugin;

    public ProductionGUI(Multigainer plugin) { this.plugin = plugin; }

    public static void open(Player player, PlayerProfile profile, Multigainer plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack pane = makePane();
        for (int i = 0; i < SIZE; i++) inv.setItem(i, pane);

        inv.setItem(SLOT_INFO,   buildInfoItem());
        inv.setItem(SLOT_WORKER, buildWorkerItem(profile));
        inv.setItem(SLOT_ENERGY, buildEnergyItem(profile));
        inv.setItem(SLOT_BACK,   makeBack());

        player.openInventory(inv);
    }

    private static ItemStack buildInfoItem() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "How Production Works");
        List<String> lore = new ArrayList<>();
        lore.add("§8§m══════════════════════");
        lore.add("§6§lWORKER SYSTEM");
        lore.add("§8§m══════════════════════");
        lore.add("§7Your §fWorker §7converts blocks into");
        lore.add("§7§fWork XP§7, which levels up the worker.");
        lore.add("§8 ");
        lore.add("§b⚡ §fEnergy");
        lore.add("§7Each worker level generates more");
        lore.add("§7§fEnergy per minute§7 passively.");
        lore.add("§7Energy is used to §5roll armor bonuses");
        lore.add("§7and §eunlock §7new armor slots.");
        lore.add("§8 ");
        lore.add("§a▶ §fSending Blocks");
        lore.add("§7Open Block Storage and §eLeft Click");
        lore.add("§7a block to send §f32§7, or §eRight Click");
        lore.add("§7the production button to send §fall§7.");
        lore.add("§7Higher-tier blocks give more XP.");
        lore.add("§8§m══════════════════════");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildWorkerItem(PlayerProfile profile) {
        int    level = profile.getWorkerLevel();
        double xp    = profile.getWorkerXp();
        double need  = ProductionManager.getXpForNextLevel(level);
        double rate  = ProductionManager.getEnergyPerMinute(level);

        ItemStack item = new ItemStack(Material.PISTON);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Worker");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.WHITE + NumberFormatter.format(new BigNumber(level)));
        lore.add(ChatColor.GRAY + "Work XP: " + ChatColor.WHITE
                + NumberFormatter.format(new BigNumber(xp))
                + ChatColor.DARK_GRAY + " / "
                + ChatColor.YELLOW + NumberFormatter.format(new BigNumber(need)));
        lore.add(ChatColor.GRAY + "Energy/min: "
                + (level == 0 ? ChatColor.DARK_GRAY + "none (need lvl 1)"
                              : ChatColor.AQUA + String.format("%.2f", rate)));
        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "Gain XP via Block Storage");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildEnergyItem(PlayerProfile profile) {
        double energy = profile.getWorkerEnergy();
        double rate   = ProductionManager.getEnergyPerMinute(profile.getWorkerLevel());

        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Energy");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Stored: " + ChatColor.YELLOW
                + NumberFormatter.format(new BigNumber(energy)));
        lore.add(ChatColor.GRAY + "Rate: " + ChatColor.AQUA
                + String.format("%.2f", rate) + ChatColor.GRAY + "/min");
        lore.add(" ");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() != SLOT_BACK) return;

        PlayerProfile profile = plugin.getPlayerDataManager().getProfile(player.getUniqueId());
        if (profile != null)
            Bukkit.getScheduler().runTask(plugin, () -> PickaxeBlockStorageGUI.open(player, profile, plugin));
    }

    private static ItemStack makePane() {
        ItemStack p = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m  = p.getItemMeta();
        if (m != null) { m.setDisplayName(" "); p.setItemMeta(m); }
        return p;
    }

    private static ItemStack makeBack() {
        ItemStack p = new ItemStack(Material.ARROW);
        ItemMeta m  = p.getItemMeta();
        if (m != null) {
            m.setDisplayName("§7« §fBack");
            m.setLore(List.of("§7Return to Block Storage"));
            p.setItemMeta(m);
        }
        return p;
    }
}
